package org.example.repository;
import org.example.model.LedgerEntry;
import java.math.BigDecimal;
import java.util.List;

public interface LedgerRepository {

    LedgerEntry save(LedgerEntry entry);
    List<LedgerEntry> findByAccountId(Long accountId);
    BigDecimal sumByAccountId(Long accountId);
}