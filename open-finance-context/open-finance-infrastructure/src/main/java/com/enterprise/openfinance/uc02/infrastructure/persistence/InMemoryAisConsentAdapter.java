package com.enterprise.openfinance.uc02.infrastructure.persistence;

import com.enterprise.openfinance.uc02.domain.model.AisConsentContext;
import com.enterprise.openfinance.uc02.domain.port.out.AisConsentPort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAisConsentAdapter implements AisConsentPort {

    private final Map<String, AisConsentContext> data = new ConcurrentHashMap<>();

    public InMemoryAisConsentAdapter() {
        seed();
    }

    @Override
    public Optional<AisConsentContext> findById(String consentId) {
        return Optional.ofNullable(data.get(consentId));
    }

    private void seed() {
        data.put("CONS-AIS-001", new AisConsentContext(
                "CONS-AIS-001",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-001", "ACC-002"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        data.put("CONS-AIS-BAL-ONLY", new AisConsentContext(
                "CONS-AIS-BAL-ONLY",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS", "READBALANCES"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        data.put("CONS-AIS-EXPIRED", new AisConsentContext(
                "CONS-AIS-EXPIRED",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-001"),
                Instant.parse("2025-01-01T00:00:00Z")
        ));
    }
}
