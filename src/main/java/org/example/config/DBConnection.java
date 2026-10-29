package org.example.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = getEnvOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/bankdb");
    private static final String USER = getEnvOrDefault("DB_USER", "postgres");
    private static final String PASSWORD = getEnvOrDefault("DB_PASSWORD", "123");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}