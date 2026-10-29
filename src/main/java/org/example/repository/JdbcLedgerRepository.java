package org.example.repository;

import org.example.model.EntryType;
import org.example.model.LedgerEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcLedgerRepository implements LedgerRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLedgerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        String sql = "INSERT INTO ledger_entries (account_id, amount, type, transaction_id) " +
                "VALUES (?, ?, ?, ?) RETURNING id, created_at";

        return jdbcTemplate.queryForObject(
                sql,
                (rs, rowNum) -> {
                    entry.setId(rs.getLong("id"));
                    entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return entry;
                },
                entry.getAccountId(),
                entry.getAmount(),
                entry.getType().name(),
                entry.getTransactionId()
        );
    }

    @Override
    public List<LedgerEntry> findByAccountId(Long accountId) {
        String sql = "SELECT id, account_id, amount, type, transaction_id, created_at " +
                "FROM ledger_entries WHERE account_id = ? ORDER BY created_at";

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), accountId);
    }

    @Override
    public BigDecimal sumByAccountId(Long accountId) {
        String sql = "SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0) " +
                "AS balance FROM ledger_entries WHERE account_id = ?";

        return jdbcTemplate.queryForObject(sql, BigDecimal.class, accountId);
    }

    private LedgerEntry mapRow(ResultSet rs) throws SQLException {
        LedgerEntry entry = new LedgerEntry();
        entry.setId(rs.getLong("id"));
        entry.setAccountId(rs.getLong("account_id"));
        entry.setAmount(rs.getBigDecimal("amount"));
        entry.setType(EntryType.valueOf(rs.getString("type")));
        entry.setTransactionId(rs.getString("transaction_id"));
        entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return entry;
    }
}