package org.example.service;

import org.example.config.DBConnection;
import org.example.dto.ReconciliationReport;

import org.example.repository.JdbcReconciliationRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReconciliationService {

    private final JdbcReconciliationRepository reconciliationRepository;


    public ReconciliationService(JdbcReconciliationRepository reconciliationRepository) {
        this.reconciliationRepository = reconciliationRepository;
    }

    public ReconciliationReport runReconciliation() {

        long fromLedgerId = reconciliationRepository.getLastToLedgerId();
        long toLedgerId = reconciliationRepository.getMaxLedgerId();
        List<String> anomalies = new ArrayList<>();

        if(fromLedgerId >= toLedgerId) {
            return new ReconciliationReport(
                    fromLedgerId, toLedgerId, true, 0,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, anomalies
            );
        }
        BigDecimal totalCredit = getTotalByEntryType("CREDIT", fromLedgerId, toLedgerId);
        BigDecimal totalDebit = getTotalByEntryType("DEBIT", fromLedgerId, toLedgerId);

        BigDecimal TOTAL = totalCredit.subtract(totalDebit);


        if (TOTAL.compareTo(BigDecimal.ZERO) != 0) {
            anomalies.add("АНОМАЛИЯ: Нарушен глобальный баланс системы! Разница (Credit - Debit) = " + TOTAL);

        }

        int accountsCheckedCount = checkPerAccountBalances(fromLedgerId, toLedgerId, anomalies);
        checkDuplicateTransactionsInRange(fromLedgerId, toLedgerId, anomalies   );
        boolean isBalanced = anomalies.isEmpty();

        ReconciliationReport report = new ReconciliationReport(
                fromLedgerId, toLedgerId, isBalanced, accountsCheckedCount,
                totalCredit, totalDebit, TOTAL, anomalies
        );

        reconciliationRepository.saveRun(report);

        return report;
    }

    private BigDecimal getTotalByEntryType(String type, long fromId, long toId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM ledger_entries WHERE UPPER(type) = ? AND id > ? AND id <= ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type);
            stmt.setLong(2, fromId);
            stmt.setLong(3, toId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
            return BigDecimal.ZERO;
        } catch (SQLException e) {


            throw new RuntimeException("Ошибка вычисления сумм по типу " + type, e);
        }
    }

    private int checkPerAccountBalances(long fromId, long toId, List<String> anomalies) {
        String sql = """
            SELECT 
                le.account_id,
                a.owner_name,
                SUM(CASE WHEN UPPER(le.type) = 'CREDIT' THEN le.amount ELSE -le.amount END) AS period_delta
            FROM ledger_entries le
            JOIN accounts a ON a.id = le.account_id
            WHERE le.id > ? AND le.id <= ?
            GROUP BY le.account_id, a.owner_name
        """;

        int accountsCount = 0;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {


            stmt.setLong(1, fromId);
            stmt.setLong(2, toId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    accountsCount++;
                    long accountId = rs.getLong("account_id");
                    String ownerName = rs.getString("owner_name");

                    BigDecimal currentFullBalance = getAccountFullBalance(conn, accountId);
                    if (currentFullBalance.compareTo(BigDecimal.ZERO) < 0) {
                        anomalies.add("АНОМАЛИЯ: Аккаунт ID=" + accountId + " (" + ownerName + ") имеет отрицательный итоговый баланс: " + currentFullBalance);
                    }

                }


            }

            return accountsCount;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при посчетной проверке балансов", e);
        }
    }

    private void checkDuplicateTransactionsInRange(long fromId, long toId, List<String> anomalies) {
        String sql = """
            SELECT transaction_id, type, COUNT(*) 
            FROM ledger_entries 
            WHERE id > ? AND id <= ?
            GROUP BY transaction_id, type 
            HAVING COUNT(*) > 1
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, fromId);
            stmt.setLong(2, toId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String txId = rs.getString("transaction_id");
                    String type = rs.getString("type");

                    anomalies.add("АНОМАЛИЯ: Найден дубликат проводки с transaction_id=" + txId + " (тип " + type + ")");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска дубликатов транзакций в диапазоне", e);
        }
    }

    private BigDecimal getAccountFullBalance(Connection conn, long accountId) throws SQLException {

        String sql = """
        SELECT COALESCE(SUM(CASE WHEN UPPER(type) = 'CREDIT' THEN amount ELSE -amount END), 0) 
        FROM ledger_entries 
        WHERE account_id = ?
    """;


        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, accountId); // Подставляем ID проверяемого аккаунта

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    return rs.getBigDecimal(1); // Возвращаем итоговую сумму
                }
            }
        }

        return BigDecimal.ZERO; // Если по какой-то причине ничего не вернулось, считаем баланс нулевым
    }
}