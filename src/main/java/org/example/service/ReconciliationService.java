package org.example.service;

import org.example.dto.ReconciliationReport;
import org.example.repository.JdbcReconciliationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReconciliationService {

    private final JdbcReconciliationRepository reconciliationRepository;
    private final JdbcTemplate jdbcTemplate;

    public ReconciliationService(JdbcReconciliationRepository reconciliationRepository, JdbcTemplate jdbcTemplate) {
        this.reconciliationRepository = reconciliationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ReconciliationReport runReconciliation() {
        long fromLedgerId = reconciliationRepository.getLastToLedgerId();
        long toLedgerId = reconciliationRepository.getMaxLedgerId();
        List<String> anomalies = new ArrayList<>();

        if (fromLedgerId >= toLedgerId) {
            return new ReconciliationReport(
                    fromLedgerId, toLedgerId, true, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, anomalies
            );
        }

        BigDecimal totalCredit = getTotalByEntryType("CREDIT", fromLedgerId, toLedgerId);
        BigDecimal totalDebit = getTotalByEntryType("DEBIT", fromLedgerId, toLedgerId);

        BigDecimal total = totalCredit.subtract(totalDebit);

        if (total.compareTo(BigDecimal.ZERO) != 0) {
            anomalies.add("АНОМАЛИЯ: Нарушен глобальный баланс системы! Разница (Credit - Debit) = " + total);
        }

        int accountsCheckedCount = checkPerAccountBalances(fromLedgerId, toLedgerId, anomalies);
        checkDuplicateTransactionsInRange(fromLedgerId, toLedgerId, anomalies);
        boolean isBalanced = anomalies.isEmpty();

        ReconciliationReport report = new ReconciliationReport(
                fromLedgerId, toLedgerId, isBalanced, accountsCheckedCount,
                totalCredit, totalDebit, total, anomalies
        );

        reconciliationRepository.saveRun(report);

        return report;
    }

    private BigDecimal getTotalByEntryType(String type, long fromId, long toId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE UPPER(type) = ? AND id > ? AND id <= ?";
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, type, fromId, toId);
        return result != null ? result : BigDecimal.ZERO;
    }

    private int checkPerAccountBalances(long fromId, long toId, List<String> anomalies) {
        String sql = """
            SELECT 
                le.account_id,
                a.owner_name
            FROM ledger_entries le
            JOIN accounts a ON a.id = le.account_id
            WHERE le.id > ? AND le.id <= ?
            GROUP BY le.account_id, a.owner_name
        """;

        AtomicInteger accountsCount = new AtomicInteger(0);

        jdbcTemplate.query(sql, rs -> {
            accountsCount.incrementAndGet();
            long accountId = rs.getLong("account_id");
            String ownerName = rs.getString("owner_name");

            BigDecimal currentFullBalance = getAccountFullBalance(accountId);
            if (currentFullBalance.compareTo(BigDecimal.ZERO) < 0) {
                anomalies.add("АНОМАЛИЯ: Аккаунт ID=" + accountId + " (" + ownerName + ") имеет отрицательный итоговый баланс: " + currentFullBalance);
            }
        }, fromId, toId);

        return accountsCount.get();
    }

    private void checkDuplicateTransactionsInRange(long fromId, long toId, List<String> anomalies) {
        String sql = """
            SELECT transaction_id, type 
            FROM ledger_entries 
            WHERE id > ? AND id <= ?
            GROUP BY transaction_id, type 
            HAVING COUNT(*) > 1
        """;

        jdbcTemplate.query(sql, rs -> {
            String txId = rs.getString("transaction_id");
            String type = rs.getString("type");
            anomalies.add("АНОМАЛИЯ: Найден дубликат проводки с transaction_id=" + txId + " (тип " + type + ")");
        }, fromId, toId);
    }

    private BigDecimal getAccountFullBalance(long accountId) {
        String sql = """
            SELECT COALESCE(SUM(CASE WHEN UPPER(type) = 'CREDIT' THEN amount ELSE -amount END), 0) 
            FROM ledger_entries 
            WHERE account_id = ?
        """;
        BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, accountId);
        return result != null ? result : BigDecimal.ZERO;
    }
}