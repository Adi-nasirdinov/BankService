package org.example.service;
import org.example.model.EntryType;
import org.example.model.LedgerEntry;
import org.example.repository.LedgerRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeLedgerRepository implements LedgerRepository {
    private final List<LedgerEntry> entries = new ArrayList<>();
    private long nextId = 1;

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        entry.setId(nextId++);
        entries.add(entry);
        return entry;
    }

    @Override
    public List<LedgerEntry> findByAccountId(Long accountId) {
        return entries.stream()
                .filter(e -> e.getAccountId().equals(accountId))
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumByAccountId(Long accountId) {
        return findByAccountId(accountId).stream()
                .map(e -> e.getType() == EntryType.CREDIT ? e.getAmount() : e.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
