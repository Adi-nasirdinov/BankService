package org.example.service;

import org.example.config.DBConnection;
import org.example.repository.JdbcIdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyIntegrationTest {

    private JdbcIdempotencyRepository idempotencyRepository;

    @BeforeEach
    void setUp() throws SQLException {
        idempotencyRepository = new JdbcIdempotencyRepository();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Очищаем старые таблицы
            stmt.execute("DROP TABLE IF EXISTS idempotency_keys CASCADE");
            stmt.execute("DROP TABLE IF EXISTS ledger_entries CASCADE");
            stmt.execute("DROP TABLE IF EXISTS accounts CASCADE");

            // 2. Создаем необходимую структуру БД
            stmt.execute("""
                CREATE TABLE accounts (
                    id BIGSERIAL PRIMARY KEY,
                    balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
                    owner_name VARCHAR(255) NOT NULL
                );

                CREATE TABLE idempotency_keys (
                    key VARCHAR(255) PRIMARY KEY,
                    account_id BIGINT REFERENCES accounts(id),
                    request_hash VARCHAR(255),
                    status VARCHAR(50) NOT NULL,
                    response_code INT,
                    response_body TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // 3. Добавляем тестовый аккаунт
            stmt.execute("INSERT INTO accounts (id, balance, owner_name) VALUES (1, 1000.00, 'Alice')");
        }
    }

    @Test
    @DisplayName("Проверка блокировки дубликатов (IN_PROGRESS) и кэширования готового ответа (COMPLETED)")
    void testIdempotencyLifecycle() {
        String key = "req-uuid-98765";
        Long accountId = 1L;
        String requestHash = "hash-sum-12345";

        // --- ЭТАП 1: Первый первичный запрос ---
        boolean firstInsert = idempotencyRepository.saveInProgress(key, accountId, requestHash);
        assertTrue(firstInsert, "Первичная запись ключа со статусом IN_PROGRESS должна пройти успешно");

        // --- ЭТАП 2: Повторный параллельный запрос с тем же ключом ---
        boolean duplicateInsert = idempotencyRepository.saveInProgress(key, accountId, requestHash);
        assertFalse(duplicateInsert, "Повторный запрос с тем же ключом должен быть заблокирован (ON CONFLICT DO NOTHING)");

        // --- ЭТАП 3: Проверяем текущее состояние записи в БД ---
        var recordInProgress = idempotencyRepository.findByKey(key);
        assertTrue(recordInProgress.isPresent(), "Запись с ключом должна существовать в БД");
        assertEquals("IN_PROGRESS", recordInProgress.get().status());

        // --- ЭТАП 4: Завершение успешной обработки и сохранение ответа ---
        String expectedResponseBody = "{\"status\":\"SUCCESS\",\"transferred\":500.00}";
        int expectedHttpCode = 200;

        idempotencyRepository.updateToCompleted(key, expectedHttpCode, expectedResponseBody);

        // --- ЭТАП 5: Проверка кэшированного результата ---
        var completedRecord = idempotencyRepository.findByKey(key);
        assertTrue(completedRecord.isPresent(), "Запись должна находиться в БД");
        assertEquals("COMPLETED", completedRecord.get().status());
        assertEquals(expectedHttpCode, completedRecord.get().responseCode());
        assertEquals(expectedResponseBody, completedRecord.get().responseBody());
    }
}