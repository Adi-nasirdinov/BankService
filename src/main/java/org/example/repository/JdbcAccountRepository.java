package org.example.repository;

import org.example.config.DBConnection;
import org.example.model.Account;

import java.math.BigDecimal;
import java.sql.*;

public class JdbcAccountRepository implements AccountRepository {




    @Override

    public Account findById(Long id) {
        String sql = "SELECT id, owner_name FROM accounts WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                return mapRow(rs);
            } else {
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске счёта", e);
        }
    }

    public Account save(Account account) {
        String sql ="insert into accounts (owner_name) values(?) returning id";


        try(Connection conn = DBConnection.getConnection();
            PreparedStatement prepst = conn.prepareStatement(sql)){

            prepst.setString(1,account.getOwner_name());


            ResultSet rs = prepst.executeQuery();
            if(rs.next()){
                account.setId(rs.getLong("id"));
            }
            return account;
        }catch(SQLException e){
            throw new RuntimeException("Ошибка при создании счета", e);
        }

    }


    private Account mapRow(ResultSet rs) throws SQLException{
        Account account = new Account();
        account.setId(rs.getLong("id"));
        account.setOwner_name(rs.getString("owner_name"));
        //account.setBalance(rs.getBigDecimal("balance"));
        return account;


    }
}
