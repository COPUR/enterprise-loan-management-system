package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateConsentContext;
import com.enterprise.openfinance.businessfinancialdata.domain.port.out.CorporateConsentPort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCorporateConsentAdapter implements CorporateConsentPort {

    private final Map<String, CorporateConsentContext> consents = new ConcurrentHashMap<>();

    public InMemoryCorporateConsentAdapter() {
        seed();
    }

    @Override
    public Optional<CorporateConsentContext> findById(String consentId) {
        return Optional.ofNullable(consents.get(consentId));
    }

    private void seed() {
        consents.put("CONS-TRSY-001", new CorporateConsentContext(
                "CONS-TRSY-001",
                "TPP-001",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-M-001", "ACC-V-101", "ACC-V-102"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        consents.put("CONS-TRSY-RESTRICTED", new CorporateConsentContext(
                "CONS-TRSY-RESTRICTED",
                "TPP-001",
                "CORP-001",
                "RESTRICTED",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        consents.put("CONS-TRSY-ACCOUNTS", new CorporateConsentContext(
                "CONS-TRSY-ACCOUNTS",
                "TPP-001",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        consents.put("CONS-TRSY-EXPIRED", new CorporateConsentContext(
                "CONS-TRSY-EXPIRED",
                "TPP-001",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS", "READBALANCES", "READTRANSACTIONS"),
                Set.of("ACC-M-001", "ACC-V-101", "ACC-V-102"),
                Instant.parse("2020-01-01T00:00:00Z")
        ));
    }
}
