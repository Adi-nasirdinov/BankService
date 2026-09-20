package org.example.service;

import org.example.model.Account;
import org.example.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class FakeAccountRepository implements AccountRepository {
    private final Map<Long, Account> storage = new HashMap<>();
    private long nextId = 1;

    public void addAccount(Account account) {
        account.setId(nextId++);
        storage.put(account.getId(), account);
    }

    @Override
    public Account findById(Long id) {
        return storage.get(id);
    }

    @Override
    public Account save(Account account) {
        account.setId(nextId++);
        storage.put(account.getId(), account);
        return account;
    }


}
