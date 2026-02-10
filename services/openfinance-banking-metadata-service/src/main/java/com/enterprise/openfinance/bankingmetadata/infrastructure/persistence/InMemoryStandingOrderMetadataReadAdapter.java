package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;
import com.enterprise.openfinance.bankingmetadata.domain.port.out.StandingOrderMetadataReadPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnProperty(prefix = "openfinance.bankingmetadata.persistence", name = "mode", havingValue = "inmemory")
public class InMemoryStandingOrderMetadataReadAdapter implements StandingOrderMetadataReadPort {

    private final List<StandingOrderMetadata> standingOrders = new ArrayList<>();

    public InMemoryStandingOrderMetadataReadAdapter() {
        seed();
    }

    @Override
    public List<StandingOrderMetadata> findByAccountId(String accountId) {
        return standingOrders.stream()
                .filter(order -> order.accountId().equals(accountId))
                .toList();
    }

    @Override
    public List<StandingOrderMetadata> findAll() {
        return List.copyOf(standingOrders);
    }

    private void seed() {
        standingOrders.add(new StandingOrderMetadata(
                "SO-001",
                "ACC-001",
                "EvryMnth",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"),
                new BigDecimal("500.00"),
                "AED"
        ));
        standingOrders.add(new StandingOrderMetadata(
                "SO-002",
                "ACC-001",
                "EvryWk",
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-11-01T00:00:00Z"),
                new BigDecimal("125.00"),
                "AED"
        ));
        standingOrders.add(new StandingOrderMetadata(
                "SO-003",
                "ACC-002",
                "EvryMnth",
                Instant.parse("2026-01-15T00:00:00Z"),
                Instant.parse("2026-09-15T00:00:00Z"),
                new BigDecimal("1000.00"),
                "USD"
        ));
    }
}
