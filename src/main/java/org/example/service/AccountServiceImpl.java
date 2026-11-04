package org.example.service;

import org.example.dto.TransferResponse;
import org.example.exceptions.AccountNotFoundException;
import org.example.model.Account;
import org.example.model.LedgerEntry;
import org.example.repository.AccountRepository;
import org.example.repository.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final LedgerRepository ledgerRepository;
    private final AccountLockManager lockManager;

    public AccountServiceImpl(AccountRepository accountRepository,
                              LedgerRepository ledgerRepository,
                              AccountLockManager lockManager) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
        this.lockManager = lockManager;
    }

    @Override
    @Transactional
    public TransferResponse transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Нельзя перевести самому себе");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        Long firstId = fromAccountId < toAccountId ? fromAccountId : toAccountId;
        Long secondId = fromAccountId < toAccountId ? toAccountId : fromAccountId;

        ReentrantLock firstLock = lockManager.getLock(firstId);
        ReentrantLock secondLock = lockManager.getLock(secondId);

        try {
            if (firstLock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    if (secondLock.tryLock(1, TimeUnit.SECONDS)) {
                        try {
                            Account fromAccount = getAccount(fromAccountId);
                            Account toAccount = getAccount(toAccountId);

                            BigDecimal fromBalanceBefore = fromAccount.getBalance();
                            BigDecimal toBalanceBefore = toAccount.getBalance();

                            LedgerEntry withdrawEntry = fromAccount.withdraw(amount);
                            LedgerEntry depositEntry = toAccount.deposit(amount);

                            ledgerRepository.save(withdrawEntry);
                            ledgerRepository.save(depositEntry);

                            BigDecimal fromBalanceAfter = fromAccount.getBalance();
                            BigDecimal toBalanceAfter = toAccount.getBalance();

                            accountRepository.save(fromAccount);
                            accountRepository.save(toAccount);

                            return new TransferResponse(
                                    fromAccountId, toAccountId,
                                    fromAccount.getOwner_name(), toAccount.getOwner_name(),
                                    amount,
                                    fromBalanceBefore, fromBalanceAfter,
                                    toBalanceBefore, toBalanceAfter
                            );
                        } finally {
                            secondLock.unlock();
                        }
                    } else {
                        throw new IllegalStateException("Не удалось заблокировать счет получателя. Попробуйте позже.");
                    }
                } finally {
                    firstLock.unlock();
                }
            } else {
                throw new IllegalStateException("Счет списания занят другой операцией. Попробуйте позже.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Операция перевода была прервана", e);
        }
    }

    @Override
    @Transactional
    public Account createAccount(String owner_name, BigDecimal initialBalance) {
        if (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Начальный баланс не может быть отрицательным");
        }

        Account account = new Account(owner_name);
        Account savedAccount = accountRepository.save(account);

        if (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            LedgerEntry entry = savedAccount.deposit(initialBalance);
            accountRepository.save(savedAccount);
            ledgerRepository.save(entry);
        }

        return savedAccount;
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    private BigDecimal getBalance(Long accountId) {
        return ledgerRepository.sumByAccountId(accountId);
    }
}