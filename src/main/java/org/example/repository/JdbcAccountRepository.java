package org.example.repository;

import org.example.config.DBConnection;
import org.example.model.Account;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class JdbcAccountRepository implements AccountRepository {

    private final LedgerRepository ledgerRepository;

    public JdbcAccountRepository(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public Optional<Account> findById(Long id) {

        String sql = "SELECT id, owner_name FROM accounts WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            } else {
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске счёта", e);
        }
    }

    @Override
    public Account save(Account account) {
        if (account.getId() == null || account.getId() == 0) {
            String insertSql = "INSERT INTO accounts (owner_name) VALUES (?) RETURNING id";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement prepst = conn.prepareStatement(insertSql)) {

                prepst.setString(1, account.getOwner_name());

                ResultSet rs = prepst.executeQuery();
                if (rs.next()) {
                    account.setId(rs.getLong("id"));
                }
                return account;
            } catch (SQLException e) {
                throw new RuntimeException("Ошибка при создании счета", e);
            }
        } else {
            String updateSql = "UPDATE accounts SET owner_name = ? WHERE id = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement prepst = conn.prepareStatement(updateSql)) {

                prepst.setString(1, account.getOwner_name());
                prepst.setLong(2, account.getId());

                prepst.executeUpdate();
                return account;
            } catch (SQLException e) {
                throw new RuntimeException("Ошибка при обновлении счета", e);
            }
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {

        String owner_name = rs.getString("owner_name");
        Long id = rs.getLong("id");

        BigDecimal currentBalance = ledgerRepository.sumByAccountId(id);
        return new Account(id, owner_name, currentBalance);
    }
}