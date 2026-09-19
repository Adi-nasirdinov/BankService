package org.example.repository;

import org.example.model.Account;

import java.math.BigDecimal;

public interface AccountRepository {
    Account findById(Long id);
    Account save(Account account);
    void updateBalance(Long id, BigDecimal newBalance);
}
