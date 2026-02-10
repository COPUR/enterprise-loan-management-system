package com.enterprise.openfinance.personalfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.port.out.BalanceReadPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Repository
@ConditionalOnProperty(prefix = "openfinance.personalfinancialdata.persistence", name = "mode", havingValue = "inmemory")
public class InMemoryBalanceReadAdapter implements BalanceReadPort {

    private final Map<String, List<BalanceSnapshot>> data = Map.of(
            "ACC-001", List.of(
                    new BalanceSnapshot("ACC-001", "InterimAvailable", new BigDecimal("1200.50"), "AED", Instant.parse("2026-02-09T09:00:00Z")),
                    new BalanceSnapshot("ACC-001", "InterimBooked", new BigDecimal("1100.50"), "AED", Instant.parse("2026-02-09T09:00:00Z"))
            ),
            "ACC-002", List.of(
                    new BalanceSnapshot("ACC-002", "InterimAvailable", new BigDecimal("800.00"), "USD", Instant.parse("2026-02-09T09:00:00Z")),
                    new BalanceSnapshot("ACC-002", "InterimBooked", new BigDecimal("700.00"), "USD", Instant.parse("2026-02-09T09:00:00Z"))
            ),
            "ACC-003", List.of(
                    new BalanceSnapshot("ACC-003", "InterimAvailable", new BigDecimal("500.00"), "AED", Instant.parse("2026-02-09T09:00:00Z")),
                    new BalanceSnapshot("ACC-003", "InterimBooked", new BigDecimal("450.00"), "AED", Instant.parse("2026-02-09T09:00:00Z"))
            )
    );

    @Override
    public List<BalanceSnapshot> findByAccountId(String accountId) {
        return data.getOrDefault(accountId, List.of());
    }
}
