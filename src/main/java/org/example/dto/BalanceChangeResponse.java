package org.example.dto;

import java.math.BigDecimal;

public class BalanceChangeResponse {
    private long account_id;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;

    public BalanceChangeResponse(long account_id, BigDecimal balanceBefore, BigDecimal balanceAfter) {

        this.account_id = account_id;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
    }

    public Long getAccount_id() {
        return account_id;
    }

    public BigDecimal getBalanceBefore() {
        return balanceBefore;

    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
}
