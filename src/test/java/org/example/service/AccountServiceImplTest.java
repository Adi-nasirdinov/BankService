package org.example.service;

import org.example.dto.BalanceChangeResponse;
import org.example.exceptions.AccountNotFoundException;
import org.example.exceptions.InsufficientFoundsException;
import org.example.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        Account account = new Account();
        account.setOwner_name("Ivan");
        repository.addAccount(account);

        BalanceChangeResponse response = service.deposit(account.getId(), new BigDecimal("100.00"));

        assertThat(response.getBalanceBefore()).isEqualByComparingTo("0.00");
        assertThat(response.getBalanceAfter()).isEqualByComparingTo("100.00");
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        Account account = new Account();
        account.setOwner_name("Ivan");
        repository.addAccount(account);

        assertThatThrownBy(() -> service.withdraw(account.getId(), new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFoundsException.class);
    }

    @Test
    void getAccountThrowsWhenNotFound() {
        assertThatThrownBy(() -> service.getAccount(999L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void withdrawIsThreadSafeUnderConcurrentAccess() throws InterruptedException {
        Account account = new Account();
        account.setOwner_name("Ivan");
        repository.addAccount(account);

        service.deposit(account.getId(), new BigDecimal("1000.00"));

        int threadCount = 20;
        BigDecimal amountPerWithdraw = new BigDecimal("10.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    service.withdraw(account.getId(), amountPerWithdraw);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        BigDecimal expectedBalance = new BigDecimal("1000.00")
                .subtract(amountPerWithdraw.multiply(new BigDecimal(threadCount)));

        BigDecimal actualBalance = ledgerRepository.sumByAccountId(account.getId());

        assertThat(actualBalance).isEqualByComparingTo(expectedBalance);
    }
}