package org.example.repository;

import org.example.dto.ReconciliationReport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReconciliationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReconciliationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long getLastToLedgerId() {
        String sql = "SELECT COALESCE(MAX(to_ledger_id), 0) FROM reconciliation_runs";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    public long getMaxLedgerId() {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM ledger_entries";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    public void saveRun(ReconciliationReport report) {
        String sql = """
            INSERT INTO reconciliation_runs (
                from_ledger_id, to_ledger_id, is_balanced, accounts_checked_count,
                total_credit, total_debit, system_delta, anomalies_count, anomalies_details
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                report.getFromLedgerId(),
                report.getToLedgerId(),
                report.isBalanced(),
                report.getAccountsCheckedCount(),
                report.getTotalCredit(),
                report.getTotalDebit(),
                report.getSystemDelta(),
                report.getAnomalies().size(),
                String.join("\n", report.getAnomalies())
        );
    }
}