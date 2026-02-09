package com.enterprise.openfinance.uc04.infrastructure.persistence;

import com.enterprise.openfinance.uc04.domain.model.MetadataConsentContext;
import com.enterprise.openfinance.uc04.domain.port.out.MetadataConsentPort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMetadataConsentAdapter implements MetadataConsentPort {

    private final Map<String, MetadataConsentContext> data = new ConcurrentHashMap<>();

    public InMemoryMetadataConsentAdapter() {
        seed();
    }

    @Override
    public Optional<MetadataConsentContext> findById(String consentId) {
        return Optional.ofNullable(data.get(consentId));
    }

    private void seed() {
        data.put("CONS-META-001", new MetadataConsentContext(
                "CONS-META-001",
                "TPP-001",
                Set.of("READTRANSACTIONS", "READPARTIES", "READMETADATA", "READSTANDINGORDERS"),
                Set.of("ACC-001", "ACC-002"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        data.put("CONS-META-TX-ONLY", new MetadataConsentContext(
                "CONS-META-TX-ONLY",
                "TPP-001",
                Set.of("READTRANSACTIONS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        data.put("CONS-META-EXPIRED", new MetadataConsentContext(
                "CONS-META-EXPIRED",
                "TPP-001",
                Set.of("READTRANSACTIONS", "READPARTIES", "READMETADATA", "READSTANDINGORDERS"),
                Set.of("ACC-001"),
                Instant.parse("2025-01-01T00:00:00Z")
        ));
    }
}
