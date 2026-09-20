package org.example.dto;

import java.math.BigDecimal;

public class TransferRequest {
    private Long toAccountId;
    private BigDecimal amount;

    public Long getToAccountId() {
        return toAccountId;
    }
    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}