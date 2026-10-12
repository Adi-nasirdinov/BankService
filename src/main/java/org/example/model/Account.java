package org.example.model;


import org.example.exceptions.InsufficientFoundsException;


import java.math.BigDecimal;
import java.util.UUID;

public class Account {
    private Long id;
    private String owner_name;
    private BigDecimal balance;


public Account(String owner_name){
    this.owner_name = owner_name;
    this.balance = BigDecimal.ZERO;
}
    public Account(Long id, String owner_name, BigDecimal balance) {
        this.id = id;
        this.owner_name = owner_name;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getOwner_name() {
        return owner_name;
    }

    public void setOwner_name(String owner_name) {
        this.owner_name = owner_name;
    }

    public BigDecimal getBalance() {
        return balance;
    }


    public LedgerEntry deposit( BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");

        }
//        BigDecimal balanceBefore = getBalance(id);
//        LedgerEntry entry = new LedgerEntry(id, amount, EntryType.CREDIT, UUID.randomUUID().toString());
//        ledgerRepository.save(entry);
//        BigDecimal balanceAfter = getBalance(id);

        this.balance = this.balance.add(amount);

        return new LedgerEntry(
                this.id,
                amount,
                EntryType.CREDIT,
                UUID.randomUUID().toString()
        );

//возвращать ledger

    }

    public LedgerEntry withdraw(BigDecimal amount) {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма должна быть положительной");
        }

        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFoundsException(id, amount, this.balance);
        }

//        LedgerEntry entry = new LedgerEntry(id, amount, EntryType.DEBIT, UUID.randomUUID().toString());
//        ledgerRepository.save(entry);
//        BigDecimal balanceAfter = getBalance(id);
        this.balance = this.balance.subtract(amount);


        return new LedgerEntry(
                this.id,
                amount,
                EntryType.DEBIT,
                UUID.randomUUID().toString()
        );


    }
}
