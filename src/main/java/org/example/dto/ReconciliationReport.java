package org.example.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReconciliationReport {
    private final LocalDateTime executedAt;
    private final long fromLedgerId;
    private final long toLedgerId;
    private final boolean isBalanced;
    private final int accountsCheckedCount;
    private final BigDecimal totalCredit;
    private final BigDecimal totalDebit;
    private final BigDecimal systemDelta;
    private final List<String> anomalies;

    public ReconciliationReport(long fromLedgerId, long toLedgerId, boolean isBalanced,
                                int accountsCheckedCount, BigDecimal totalCredit,
                                BigDecimal totalDebit, BigDecimal systemDelta,
                                List<String> anomalies) {
        this.executedAt = LocalDateTime.now();
        this.fromLedgerId = fromLedgerId;
        this.toLedgerId = toLedgerId;
        this.isBalanced = isBalanced;
        this.accountsCheckedCount = accountsCheckedCount;
        this.totalCredit = totalCredit;
        this.totalDebit = totalDebit;
        this.systemDelta = systemDelta;
        this.anomalies = anomalies;
    }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public long getFromLedgerId() { return fromLedgerId; }
    public long getToLedgerId() { return toLedgerId; }
    public boolean isBalanced() { return isBalanced; }
    public int getAccountsCheckedCount() { return accountsCheckedCount; }
    public BigDecimal getTotalCredit() { return totalCredit; }
    public BigDecimal getTotalDebit() { return totalDebit; }
    public BigDecimal getSystemDelta() { return systemDelta; }
    public List<String> getAnomalies() { return anomalies; }

    @Override
    public String toString() {
        return "ReconciliationReport{" +
                "executedAt=" + executedAt +
                ", range=[" + fromLedgerId + " -> " + toLedgerId + "]" +
                ", isBalanced=" + isBalanced +
                ", accountsChecked=" + accountsCheckedCount +
                ", systemDelta=" + systemDelta +
                ", anomaliesCount=" + anomalies.size() +
                '}';
    }
}