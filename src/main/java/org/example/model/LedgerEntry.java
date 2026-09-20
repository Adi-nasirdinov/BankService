package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerEntry {

    private Long id;
    private Long accountId;
    private BigDecimal amount;
    private EntryType type;
    private String transactionId;
    private LocalDateTime createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(Long accountId, BigDecimal amount, EntryType type, String transactionId) {
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.transactionId = transactionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}