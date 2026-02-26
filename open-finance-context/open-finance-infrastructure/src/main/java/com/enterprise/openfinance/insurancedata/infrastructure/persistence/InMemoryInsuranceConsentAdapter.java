package com.enterprise.openfinance.insurancedata.infrastructure.persistence;

import com.enterprise.openfinance.insurancedata.domain.model.InsuranceConsentContext;
import com.enterprise.openfinance.insurancedata.domain.port.out.InsuranceConsentPort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryInsuranceConsentAdapter implements InsuranceConsentPort {

    private final Map<String, InsuranceConsentContext> data = new ConcurrentHashMap<>();

    public InMemoryInsuranceConsentAdapter() {
        data.put("CONS-INS-001", new InsuranceConsentContext(
                "CONS-INS-001",
                "TPP-001",
                Set.of("ReadPolicies"),
                Set.of("POL-MTR-001", "POL-MTR-002"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        data.put("CONS-INS-INACTIVE", new InsuranceConsentContext(
                "CONS-INS-INACTIVE",
                "TPP-001",
                Set.of("ReadPolicies"),
                Set.of("POL-MTR-003"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));

        data.put("CONS-INS-EXPIRED", new InsuranceConsentContext(
                "CONS-INS-EXPIRED",
                "TPP-001",
                Set.of("ReadPolicies"),
                Set.of("POL-MTR-001"),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

        data.put("CONS-INS-RO", new InsuranceConsentContext(
                "CONS-INS-RO",
                "TPP-001",
                Set.of("ReadBalances"),
                Set.of("POL-MTR-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        ));
    }

    @Override
    public Optional<InsuranceConsentContext> findById(String consentId) {
        return Optional.ofNullable(data.get(consentId));
    }
}
