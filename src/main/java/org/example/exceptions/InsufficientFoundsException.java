package org.example.exceptions;

import java.math.BigDecimal;

public class InsufficientFoundsException extends RuntimeException {
    private final Long id;
    private final BigDecimal requested;
    private final BigDecimal available;

    public InsufficientFoundsException(Long id, BigDecimal requested, BigDecimal available) {
        super(String.format(" %d. Запрошено: %s, Доступно: %s", id, requested, available));
        this.id = id;
        this.requested = requested;
        this.available = available;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getRequested() {
        return requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }
}