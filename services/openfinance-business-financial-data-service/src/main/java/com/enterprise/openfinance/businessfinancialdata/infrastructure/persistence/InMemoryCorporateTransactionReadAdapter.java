package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateTransactionReadPort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Repository
public class InMemoryCorporateTransactionReadAdapter implements CorporateTransactionReadPort {

    private final List<CorporateTransactionSnapshot> transactions = new ArrayList<>();

    public InMemoryCorporateTransactionReadAdapter() {
        seed();
    }

    @Override
    public List<CorporateTransactionSnapshot> findByAccountIds(Set<String> accountIds) {
        return transactions.stream()
                .filter(tx -> accountIds.contains(tx.accountId()))
                .sorted(Comparator.comparing(CorporateTransactionSnapshot::bookingDateTime).reversed())
                .toList();
    }

    private void seed() {
        transactions.add(new CorporateTransactionSnapshot(
                "TX-TRSY-001",
                "ACC-M-001",
                new BigDecimal("3500.00"),
                "AED",
                Instant.parse("2026-02-05T00:00:00Z"),
                "SWEEP",
                "ZBA",
                "Daily concentration sweep"
        ));

        transactions.add(new CorporateTransactionSnapshot(
                "TX-TRSY-002",
                "ACC-M-001",
                new BigDecimal("450.00"),
                "AED",
                Instant.parse("2026-02-03T00:00:00Z"),
                "BOOK",
                null,
                "Supplier settlement"
        ));

        transactions.add(new CorporateTransactionSnapshot(
                "TX-TRSY-003",
                "ACC-V-101",
                new BigDecimal("1100.00"),
                "AED",
                Instant.parse("2026-02-04T00:00:00Z"),
                "SWEEP",
                "ZBA",
                "Virtual account sweep"
        ));

        transactions.add(new CorporateTransactionSnapshot(
                "TX-TRSY-004",
                "ACC-V-102",
                new BigDecimal("800.00"),
                "AED",
                Instant.parse("2026-01-15T00:00:00Z"),
                "BOOK",
                null,
                "Customer collection"
        ));

        transactions.add(new CorporateTransactionSnapshot(
                "TX-TRSY-005",
                "ACC-M-002",
                new BigDecimal("600.00"),
                "USD",
                Instant.parse("2026-02-01T00:00:00Z"),
                "BOOK",
                null,
                "Treasury transfer"
        ));
    }
}
