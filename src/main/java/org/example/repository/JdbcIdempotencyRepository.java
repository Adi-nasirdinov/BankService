package org.example.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcIdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcIdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record IdempotencyRecord(
            String key,
            String status,
            Integer responseCode,
            String responseBody
    ) {}

    public boolean saveInProgress(String key, Long accountId, String requestHash) {
        String sql = "INSERT INTO idempotency_keys (key, account_id, request_hash, status) " +
                "VALUES (?, ?, ?, 'IN_PROGRESS') ON CONFLICT (key) DO NOTHING";

        int rowsAffected = jdbcTemplate.update(sql, key, accountId, requestHash);
        return rowsAffected > 0;
    }

    public Optional<IdempotencyRecord> findByKey(String key) {
        String sql = "SELECT key, status, response_code, response_body FROM idempotency_keys WHERE key = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> new IdempotencyRecord(
                rs.getString("key"),
                rs.getString("status"),
                rs.getObject("response_code", Integer.class),
                rs.getString("response_body")
        ), key).stream().findFirst();
    }

    public void updateToCompleted(String key, int responseCode, String responseBody) {
        String sql = "UPDATE idempotency_keys SET status = 'COMPLETED', response_code = ?, response_body = ? WHERE key = ?";

        jdbcTemplate.update(sql, responseCode, responseBody, key);
    }
}