package org.example.service;

import org.example.config.DBConnection;
import org.example.dto.ReconciliationReport;
import org.example.repository.JdbcReconciliationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationWatermarkTest {

    private ReconciliationService reconciliationService;
    private JdbcReconciliationRepository reconciliationRepository;

    @BeforeEach
    void setUp() throws SQLException {
        reconciliationRepository = new JdbcReconciliationRepository();
        reconciliationService = new ReconciliationService(reconciliationRepository);

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Удаляем таблицы с устаревшей структурой
            stmt.execute("DROP TABLE IF EXISTS reconciliation_runs CASCADE");
            stmt.execute("DROP TABLE IF EXISTS ledger_entries CASCADE");
            stmt.execute("DROP TABLE IF EXISTS accounts CASCADE");

            // 2. Создаем таблицы с точными названиями колонок под JdbcReconciliationRepository
            stmt.execute("""
            CREATE TABLE accounts (
                id BIGSERIAL PRIMARY KEY,
                balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
                owner_name VARCHAR(255) NOT NULL
            );
            
            CREATE TABLE ledger_entries (
                id BIGSERIAL PRIMARY KEY,
                account_id BIGINT REFERENCES accounts(id),
                amount NUMERIC(19, 2) NOT NULL,
                type VARCHAR(10) NOT NULL,
                transaction_id VARCHAR(255) NOT NULL
            );

            CREATE TABLE reconciliation_runs (
                id BIGSERIAL PRIMARY KEY,
                from_ledger_id BIGINT NOT NULL,
                to_ledger_id BIGINT NOT NULL,
                is_balanced BOOLEAN NOT NULL,
                accounts_checked_count INT NOT NULL,
                total_credit NUMERIC(19, 2) NOT NULL,
                total_debit NUMERIC(19, 2) NOT NULL,
                system_delta NUMERIC(19, 2) NOT NULL,
                anomalies_count INT NOT NULL,           -- Добавлено
                anomalies_details TEXT,                -- Добавлено
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """);
        }

    }
    @Test
    @DisplayName("Проверка инкрементального сдвига Watermark и фиксации аномалий")
    void testWatermarkAndAnomalies() throws SQLException {
        // Подготовка данных: передаем balance = 1000.00
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO accounts (id, balance, owner_name) VALUES (1, 1000.00, 'Test User')");
            stmt.execute("INSERT INTO ledger_entries (account_id, amount, type, transaction_id) " +
                    "VALUES (1, 1000.00, 'CREDIT', 'tx-100')");
        }

        // Первый запуск
        ReconciliationReport firstRunReport = reconciliationService.runReconciliation();

        assertFalse(firstRunReport.isBalanced(), "Первый запуск должен зафиксировать аномалию");
        assertEquals(0, firstRunReport.getFromLedgerId(), "Старт с 0");
        assertTrue(firstRunReport.getToLedgerId() > 0, "Финишный ID должен сдвинуться");
        assertFalse(firstRunReport.getAnomalies().isEmpty(), "Список аномалий не пуст");

        long lastCheckpoint = firstRunReport.getToLedgerId();

        // Второй запуск
        ReconciliationReport secondRunReport = reconciliationService.runReconciliation();

        assertTrue(secondRunReport.isBalanced(), "Второй запуск должен быть сбалансирован (нет новых данных)");
        assertEquals(lastCheckpoint, secondRunReport.getFromLedgerId(), "fromLedgerId равен прошлому checkpoint");
        assertEquals(lastCheckpoint, secondRunReport.getToLedgerId(), "toLedgerId не изменился");
        assertTrue(secondRunReport.getAnomalies().isEmpty(), "Новых аномалий нет");
    }
}