package org.example.repository;

import org.example.model.Account;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findById(Long id);

    Account save(Account account);

}
