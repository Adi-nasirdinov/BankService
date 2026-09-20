package org.example.dto;

import java.math.BigDecimal;

public class TransferResponse {
    private Long fromAccountId;
    private Long toAccountId;
    private String fromOwnerName;
    private String toOwnerName;
    private BigDecimal amount;
    private BigDecimal fromBalanceBefore;
    private BigDecimal toBalanceBefore;
    private BigDecimal fromBalanceAfter;
    private BigDecimal toBalanceAfter;

    public TransferResponse(Long fromAccountId, Long toAccountId,
                            String fromOwnerName, String toOwnerName,
                            BigDecimal amount,
                            BigDecimal fromBalanceBefore, BigDecimal fromBalanceAfter,
                            BigDecimal toBalanceBefore, BigDecimal toBalanceAfter) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.fromOwnerName = fromOwnerName;
        this.toOwnerName = toOwnerName;
        this.amount = amount;
        this.fromBalanceBefore = fromBalanceBefore;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceBefore = toBalanceBefore;
        this.toBalanceAfter = toBalanceAfter;
    }

    public Long getFromAccountId() {
        return fromAccountId; }
    public Long getToAccountId() {
        return toAccountId; }

        public String getFromOwnerName() {
        return fromOwnerName; }
        public String getToOwnerName() {
        return toOwnerName; }

    public BigDecimal getAmount() {
        return amount; }

        public BigDecimal getFromBalanceBefore() {
        return fromBalanceBefore; }
        public BigDecimal getFromBalanceAfter() {
        return fromBalanceAfter; }
        public BigDecimal getToBalanceBefore() {
        return toBalanceBefore; }
        public BigDecimal getToBalanceAfter() {
        return toBalanceAfter; }
}
