package org.example.repository;

import org.example.model.Account;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class JdbcAccountRepository implements AccountRepository {

    private final JdbcTemplate jdbcTemplate;
    private final LedgerRepository ledgerRepository;

    public JdbcAccountRepository(JdbcTemplate jdbcTemplate, LedgerRepository ledgerRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    public Optional<Account> findById(Long id) {
        String sql = "SELECT id, owner_name FROM accounts WHERE id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), id)
                .stream()
                .findFirst();
    }

    @Override
    public Account save(Account account) {
        if (account.getId() == null || account.getId() == 0) {
            String insertSql = "INSERT INTO accounts (owner_name) VALUES (?) RETURNING id";

            Long generatedId = jdbcTemplate.queryForObject(insertSql, Long.class, account.getOwner_name());
            account.setId(generatedId);
            return account;
        } else {
            String updateSql = "UPDATE accounts SET owner_name = ? WHERE id = ?";

            jdbcTemplate.update(updateSql, account.getOwner_name(), account.getId());
            return account;
        }
    }

    private Account mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String ownerName = rs.getString("owner_name");

        BigDecimal currentBalance = ledgerRepository.sumByAccountId(id);
        return new Account(id, ownerName, currentBalance);
    }
}