package org.example.repository;

import org.example.config.DBConnection;

import java.sql.*;
import java.util.Optional;

public class JdbcIdempotencyRepository {

    public record IdempotencyRecord(
            String key,
            String status,
            Integer responseCode,
            String responseBody
    ) {}


    public boolean saveInProgress(String key, Long accountId, String requestHash) {
        String sql = "INSERT INTO idempotency_keys (key, account_id, request_hash, status) " +
                "VALUES (?, ?, ?, 'IN_PROGRESS') ON CONFLICT (key) DO NOTHING";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            stmt.setLong(2, accountId);
            stmt.setString(3, requestHash);

            return stmt.executeUpdate() > 0; // true, если строка вставлена
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка записи idempotency key", e);
        }
    }

    public Optional<IdempotencyRecord> findByKey(String key) {
        String sql = "SELECT key, status, response_code, response_body FROM idempotency_keys WHERE key = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new IdempotencyRecord(
                        rs.getString("key"),
                        rs.getString("status"),
                        rs.getObject("response_code", Integer.class),
                        rs.getString("response_body")
                ));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска idempotency record", e);
        }
    }

    public void updateToCompleted(String key, int responseCode, String responseBody) {
        String sql = "UPDATE idempotency_keys SET status = 'COMPLETED', response_code = ?, response_body = ? WHERE key = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, responseCode);
            stmt.setString(2, responseBody);
            stmt.setString(3, key);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка обновления idempotency record", e);
        }
    }
}