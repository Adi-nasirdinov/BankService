package org.example.repository;
import org.example.config.DBConnection;
import org.example.model.EntryType;
import org.example.model.LedgerEntry;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcLedgerRepository implements LedgerRepository {

    public LedgerEntry save(LedgerEntry entry) {
        String sql = "INSERT INTO ledger_entries (account_id, amount, type, transaction_id) " +
                "VALUES (?, ?, ?, ?) RETURNING id, created_at";


           try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {


            stmt.setLong(1, entry.getAccountId());
            stmt.setBigDecimal(2, entry.getAmount());
            stmt.setString(3, entry.getType().name());
            stmt.setString(4, entry.getTransactionId());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                entry.setId(rs.getLong("id"));
                entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }

            return entry;

        } catch (SQLException e) {

            throw new RuntimeException("Ошибка при сохранении записи в журнал", e);
        }
    }

    @Override
    public List<LedgerEntry> findByAccountId(Long accountId) {
        String sql = "SELECT id, account_id, amount, type, transaction_id, created_at " +
                "FROM ledger_entries WHERE account_id = ? ORDER BY created_at";

        List<LedgerEntry> entries = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                entries.add(mapRow(rs));
            }

            return entries;

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске записей журнала", e);

        }
    }




    public BigDecimal sumByAccountId(Long accountId) {
        String sql = "SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END), 0) " +
                "AS balance FROM ledger_entries WHERE account_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, accountId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("balance");
            }

            return BigDecimal.ZERO;


        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при подсчёте баланса", e);
        }
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