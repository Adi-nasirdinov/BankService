package org.example.service;

import java.util.concurrent.ConcurrentHashMap;

public class AccountLockManager {

    private final ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();
    public Object getLock(Long accountId) {
        return locks.computeIfAbsent(accountId, id -> new Object());
    }
}
