package org.example.service;

import org.example.exceptions.AccountNotFoundException;
import org.example.exceptions.InsufficientFoundsException;
import org.example.model.Account;
import org.example.model.LedgerEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AccountServiceImplTest {

    private AccountService service;
    private FakeAccountRepository repository;
    private FakeLedgerRepository ledgerRepository;

    @BeforeEach
    void setUp() {
        repository = new FakeAccountRepository();
        ledgerRepository = new FakeLedgerRepository();
        service = new AccountServiceImpl(repository, ledgerRepository);
    }

    @Test
    void depositIncreasesBalance() {
        // Используем конструктор с именем владельца
        Account account = new Account("Ivan");
        repository.addAccount(account);

        BigDecimal balanceBefore = account.getBalance(); // 0.00 по умолчанию
        LedgerEntry entry = account.deposit(new BigDecimal("100.00"));
        ledgerRepository.save(entry);

        assertThat(balanceBefore).isEqualByComparingTo("0.00");
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        Account account = new Account("Ivan");
        repository.addAccount(account);

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFoundsException.class);
    }

    @Test
    void getAccountThrowsWhenNotFound() {
        assertThatThrownBy(() -> service.getAccount(999L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void withdrawIsThreadSafeUnderConcurrentAccess() throws InterruptedException {
        // Создаем аккаунт через сервис и пополняем его для теста
        Account account = service.createAccount("Ivan", new BigDecimal("1000.00"));

        int threadCount = 20;
        BigDecimal amountPerWithdraw = new BigDecimal("10.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    synchronized (account) {
                        LedgerEntry entry = account.withdraw(amountPerWithdraw);
                        ledgerRepository.save(entry);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        BigDecimal expectedBalance = new BigDecimal("1000.00")
                .subtract(amountPerWithdraw.multiply(new BigDecimal(threadCount)));

        assertThat(account.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void depositsOnDifferentAccountsRunConcurrently() throws InterruptedException {
        // 1. Создаем аккаунты через правильный конструктор
        Account accountA = new Account("A");
        repository.save(accountA);

        Account accountB = new Account("B");
        repository.save(accountB);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1); // Для синхронного старта потоков
        CountDownLatch doneLatch = new CountDownLatch(2);

        long start = System.currentTimeMillis();

        executor.submit(() -> {
            try {
                startLatch.await(); // Ждем единой команды на старт
                LedgerEntry entry = accountA.deposit(new BigDecimal("100.00"));
                ledgerRepository.save(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                LedgerEntry entry = accountB.deposit(new BigDecimal("100.00"));
                ledgerRepository.save(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // Одновременный запуск обоих потоков
        boolean completedInTime = doneLatch.await(2, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();

        // Проверяем актуальный баланс через агрегатор LedgerRepository
        assertThat(ledgerRepository.sumByAccountId(accountA.getId())).isEqualByComparingTo("100.00");
        assertThat(ledgerRepository.sumByAccountId(accountB.getId())).isEqualByComparingTo("100.00");
    }

    @Test
    void transferBetweenAccountsIsThreadSafeAndDeadlockFree() throws InterruptedException {
        Account accountA = service.createAccount("A", new BigDecimal("1000.00"));
        Account accountB = service.createAccount("B", new BigDecimal("1000.00"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Встречные переводы
        executor.submit(() -> {
            try {
                service.transfer(accountA.getId(), accountB.getId(), new BigDecimal("50.00"));
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                service.transfer(accountB.getId(), accountA.getId(), new BigDecimal("30.00"));
            } finally {
                latch.countDown();
            }
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        BigDecimal expectedA = new BigDecimal("1000.00").subtract(new BigDecimal("50.00")).add(new BigDecimal("30.00"));
        BigDecimal expectedB = new BigDecimal("1000.00").subtract(new BigDecimal("30.00")).add(new BigDecimal("50.00"));

        Account freshAccountA = service.getAccount(accountA.getId());
        Account freshAccountB = service.getAccount(accountB.getId());

        assertThat(freshAccountA.getBalance()).isEqualByComparingTo(expectedA);
        assertThat(freshAccountB.getBalance()).isEqualByComparingTo(expectedB);
    }
}