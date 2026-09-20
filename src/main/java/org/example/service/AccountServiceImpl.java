package org.example.service;


import org.example.dto.TransferResponse;
import org.example.exceptions.AccountNotFoundException;
import org.example.exceptions.InsufficientFoundsException;
import org.example.model.Account;
import org.example.model.EntryType;
import org.example.model.LedgerEntry;
import org.example.repository.AccountRepository;
import org.example.dto.BalanceChangeResponse;
import org.example.repository.LedgerRepository;

import java.math.BigDecimal;
import java.util.UUID;

public class AccountServiceImpl  implements AccountService {
    final private AccountRepository repository;
    private final LedgerRepository ledgerRepository;

    public AccountServiceImpl(AccountRepository repository,  LedgerRepository ledgerRepository) {
        this.repository = repository;
        this.ledgerRepository = ledgerRepository;
    }


    public synchronized TransferResponse transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Нельзя перевести самому себе");
        }

        Account fromAccount = repository.findById(fromAccountId);
        if (fromAccount == null) {
            throw new AccountNotFoundException(fromAccountId);
        }

        Account toAccount = repository.findById(toAccountId);
        if (toAccount == null) {
            throw new AccountNotFoundException(toAccountId);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        BigDecimal fromBalanceBefore = getBalance(fromAccountId);
        BigDecimal toBalanceBefore = getBalance(toAccountId);

        if (fromBalanceBefore.compareTo(amount) < 0) {
            throw new InsufficientFoundsException(fromAccountId, amount, fromBalanceBefore);
        }

        String transactionId = UUID.randomUUID().toString();

        LedgerEntry debitEntry = new LedgerEntry(fromAccountId, amount, EntryType.DEBIT, transactionId);
        ledgerRepository.save(debitEntry);

        LedgerEntry creditEntry = new LedgerEntry(toAccountId, amount, EntryType.CREDIT, transactionId);
        ledgerRepository.save(creditEntry);

        BigDecimal fromBalanceAfter = getBalance(fromAccountId);
        BigDecimal toBalanceAfter = getBalance(toAccountId);

        return new TransferResponse(
                fromAccountId, toAccountId,
                fromAccount.getOwner_name(), toAccount.getOwner_name(),
                amount,
                fromBalanceBefore, fromBalanceAfter,
                toBalanceBefore, toBalanceAfter
        );
    }

    public Account createAccount(String owner_name,BigDecimal initialBalance) {
        if(initialBalance.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Новый баланс нн может быть ниже нуля");
        }
        Account account = new Account();
        account.setOwner_name(owner_name);

        Account saved = repository.save(account);

        if(initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            LedgerEntry entry = new LedgerEntry(
                    saved.getId(),
                    initialBalance,
                    EntryType.CREDIT,
                    UUID.randomUUID().toString()

            );

            ledgerRepository.save(entry);
        }

        saved.setBalance(getBalance(saved.getId()));
        return saved;
    }

    public synchronized BalanceChangeResponse deposit(Long id,  BigDecimal amount) {

        Account account = repository.findById(id);
        if(account == null){
            throw new AccountNotFoundException(id);
        }
        if(amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Сумма должна быть положительной");

        }


          BigDecimal balanceBefore = getBalance(id);
        LedgerEntry entry = new LedgerEntry(id,amount,EntryType.CREDIT, UUID.randomUUID().toString());
        ledgerRepository.save(entry);
        BigDecimal balanceAfter = getBalance(id);




        return new BalanceChangeResponse(id, balanceBefore, balanceAfter);
    }

    public synchronized BalanceChangeResponse withdraw(Long id,  BigDecimal amount) {
        Account account = repository.findById(id);

        if(account == null){
            throw new AccountNotFoundException(id);
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

//        if (account.getBalance().compareTo(amount) < 0) {
//            throw new InsufficientFoundsException(id, amount, account.getBalance());
//        }

        BigDecimal balanceBefore = getBalance(id);






            if(balanceBefore.compareTo(amount) < 0) {


                throw new InsufficientFoundsException(id,amount, balanceBefore);
        }

            LedgerEntry entry = new LedgerEntry(id,amount,EntryType.DEBIT, UUID.randomUUID().toString());
            ledgerRepository.save(entry);
        BigDecimal balanceAfter = getBalance(id);



        return new BalanceChangeResponse(id, balanceBefore, balanceAfter);




    }
    public Account getAccount(Long id){
        Account account;
        account = repository.findById(id);
        if(account == null){
            throw new AccountNotFoundException(id);
        }
        account.setBalance(getBalance(id));
        return account;
    }

    private BigDecimal getBalance(Long accountId) {
        return ledgerRepository.sumByAccountId(accountId);
    }

}
