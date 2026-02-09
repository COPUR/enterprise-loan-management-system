package com.enterprise.openfinance.uc05.infrastructure.persistence;

import com.enterprise.openfinance.uc05.domain.model.CorporateBalanceSnapshot;
import com.enterprise.openfinance.uc05.domain.port.out.CorporateBalanceReadPort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCorporateBalanceReadAdapter implements CorporateBalanceReadPort {

    private final Map<String, List<CorporateBalanceSnapshot>> balancesByMaster = new ConcurrentHashMap<>();

    public InMemoryCorporateBalanceReadAdapter() {
        seed();
    }

    @Override
    public List<CorporateBalanceSnapshot> findByMasterAccountId(String masterAccountId) {
        return balancesByMaster.getOrDefault(masterAccountId, List.of());
    }

    private void seed() {
        balancesByMaster.put("ACC-M-001", List.of(
                new CorporateBalanceSnapshot(
                        "ACC-M-001",
                        "InterimAvailable",
                        new BigDecimal("15000.00"),
                        "AED",
                        Instant.parse("2026-02-09T10:00:00Z")
                ),
                new CorporateBalanceSnapshot(
                        "ACC-M-001",
                        "InterimBooked",
                        new BigDecimal("14250.35"),
                        "AED",
                        Instant.parse("2026-02-09T10:00:00Z")
                )
        ));

        balancesByMaster.put("ACC-M-002", List.of(
                new CorporateBalanceSnapshot(
                        "ACC-M-002",
                        "InterimAvailable",
                        new BigDecimal("5000.00"),
                        "USD",
                        Instant.parse("2026-02-09T10:00:00Z")
                )
        ));
    }
}
