package org.example.service;

import org.example.dto.BalanceChangeResponse;
import org.example.dto.TransferResponse;
import org.example.model.Account;

import java.math.BigDecimal;

public interface AccountService {

    Account getAccount(Long id);
    Account createAccount(String owner_name, BigDecimal balance);

    TransferResponse transfer(Long fromAccountId, Long toAccountId, BigDecimal amount);
}
