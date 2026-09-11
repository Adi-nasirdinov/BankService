package org.example.service;

import org.example.dto.BalanceChangeResponse;
import org.example.exceptions.AccountNotFoundException;
import org.example.exceptions.InsufficientFoundsException;
import org.example.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class AccountServiceImplTest {
    private AccountService service;
    private FakeAccountRepository repository;

    @BeforeEach
    void setUp() {
        repository = new FakeAccountRepository();
        service = new AccountServiceImpl(repository);
    }

    @Test
    void depositeIncreaseBalance() {
        Account account = new Account();
        account.setOwner_name("Vova");
        account.setBalance(new BigDecimal("1000.00"));
        repository.addAccount(account);

        BalanceChangeResponse response = service.deposit(account.getId(), new BigDecimal("100.00"));

        assertThat(response.getBalanceBefore()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(response.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("1100.00"));

    }

    @Test
    void withdrawDecreaseBalance() {
        Account account = new Account();
        account.setOwner_name("Vova");
        account.setBalance(new BigDecimal("1000.00"));
        repository.addAccount(account);

        BalanceChangeResponse response = service.withdraw(account.getId(), new BigDecimal("300.00"));
        assertThat(response.getBalanceAfter()).isEqualByComparingTo("700.00");
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        Account account = new Account();
        account.setOwner_name("Ivan");
        account.setBalance(new BigDecimal("100.00"));
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
    void depositThrowsWhenAmountIsNegative() {
        Account account = new Account();
        account.setBalance(new BigDecimal("1000.00"));
        repository.addAccount(account);

        assertThatThrownBy(() -> service.deposit(account.getId(), new BigDecimal("-50.00")))
                .isInstanceOf(IllegalArgumentException.class);

    }
}
