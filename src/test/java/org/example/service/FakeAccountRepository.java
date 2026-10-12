package org.example.service;

import org.example.model.Account;
import org.example.repository.AccountRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FakeAccountRepository implements AccountRepository {
    private final Map<Long, Account> storage = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /**
     * Вспомогательный метод для тестов
     */
    public void addAccount(Account account) {
        save(account);
    }

    @Override
    public Optional<Account> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Account save(Account account) {
        // Если ID еще не присвоен (новый аккаунт), генерируем новый
        if (account.getId() == null) {
            account.setId(nextId.getAndIncrement());
        }

        // Сохраняем или обновляем аккаунт по его текущему ID
        storage.put(account.getId(), account);
        return account;
    }
}