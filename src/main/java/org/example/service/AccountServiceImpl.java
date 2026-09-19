package org.example.service;


import org.example.exceptions.AccountNotFoundException;
import org.example.exceptions.InsufficientFoundsException;
import org.example.model.Account;
import org.example.repository.AccountRepository;
import org.example.dto.BalanceChangeResponse;

import java.math.BigDecimal;

public class AccountServiceImpl  implements AccountService {
    final private AccountRepository repository;

    public AccountServiceImpl(AccountRepository repository) {
        this.repository = repository;
    }

    public Account createAccount(String owner_name,BigDecimal initialBalance) {
        if(initialBalance.compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Новый баланс нн может быть ниже нуля");
        }
        Account account = new Account();
        account.setOwner_name(owner_name);
        account.setBalance(initialBalance);
        return repository.save(account);
    }

    public synchronized BalanceChangeResponse deposit(Long id,  BigDecimal amount) {

        Account account = repository.findById(id);
        if(account == null){
            throw new AccountNotFoundException(id);
        }
        if(amount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Сумма должна быть положительной");

        }


          BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);
           repository.updateBalance(id, balanceAfter);



        return new BalanceChangeResponse(id, balanceBefore, balanceAfter);
    }

    public synchronized BalanceChangeResponse withdraw(Long id,  BigDecimal amount) {
        Account account = repository.findById(id);

        if(account == null){
            throw new AccountNotFoundException(id);
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFoundsException(id, amount, account.getBalance());
        }

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter;





            if(balanceBefore.compareTo(amount) >= 0) {
                balanceBefore = account.getBalance();
                balanceAfter = balanceBefore.subtract(amount);
            }else{
                throw new InsufficientFoundsException(id,amount, balanceBefore);
        }

            repository.updateBalance(id, balanceAfter);

        return new BalanceChangeResponse(id, balanceBefore, balanceAfter);




    }
    public Account getAccount(Long id){
        Account account;
        account = repository.findById(id);
        if(account == null){
            throw new AccountNotFoundException(id);
        }
        return account;
    }

}
