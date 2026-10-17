package org.example.repository;

import org.example.config.DBConnection;
import org.example.dto.ReconciliationReport;

import java.sql.*;

public class JdbcReconciliationRepository {

    public long getLastToLedgerId() {
        String sql = "SELECT COALESCE(MAX(to_ledger_id), 0) FROM reconciliation_runs";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении контрольной точки реконсиляции", e);
        }
    }

    public long getMaxLedgerId() {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM ledger_entries";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении максимального ID проводки", e);
        }
    }

    public void saveRun(ReconciliationReport report) {
        String sql = """
            INSERT INTO reconciliation_runs (
                from_ledger_id, to_ledger_id, is_balanced, accounts_checked_count,
                total_credit, total_debit, system_delta, anomalies_count, anomalies_details
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, report.getFromLedgerId());
            stmt.setLong(2, report.getToLedgerId());
            stmt.setBoolean(3, report.isBalanced());
            stmt.setInt(4, report.getAccountsCheckedCount());
            stmt.setBigDecimal(5, report.getTotalCredit());
            stmt.setBigDecimal(6, report.getTotalDebit());
            stmt.setBigDecimal(7, report.getSystemDelta());
            stmt.setInt(8, report.getAnomalies().size());
            stmt.setString(9, String.join("\n", report.getAnomalies()));

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения результатов реконсиляции в БД", e);
        }
    }
}