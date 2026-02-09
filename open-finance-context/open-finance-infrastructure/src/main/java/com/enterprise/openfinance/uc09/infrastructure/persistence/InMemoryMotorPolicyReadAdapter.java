package com.enterprise.openfinance.uc09.infrastructure.persistence;

import com.enterprise.openfinance.uc09.domain.model.MotorPolicy;
import com.enterprise.openfinance.uc09.domain.model.MotorPolicyStatus;
import com.enterprise.openfinance.uc09.domain.port.out.MotorPolicyReadPort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMotorPolicyReadAdapter implements MotorPolicyReadPort {

    private final Map<String, MotorPolicy> policies = new ConcurrentHashMap<>();

    public InMemoryMotorPolicyReadAdapter() {
        policies.put("POL-MTR-001", new MotorPolicy(
                "POL-MTR-001",
                "MTR-2026-0001",
                "Ali Copur",
                null,
                "Toyota",
                "Camry",
                2023,
                new BigDecimal("1890.50"),
                "AED",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                MotorPolicyStatus.ACTIVE,
                List.of("Collision", "Theft")
        ));

        policies.put("POL-MTR-002", new MotorPolicy(
                "POL-MTR-002",
                "MTR-2026-0002",
                "Amina Saleh",
                null,
                "Honda",
                "Accord",
                2022,
                new BigDecimal("1725.00"),
                "AED",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                MotorPolicyStatus.ACTIVE,
                List.of("ThirdParty", "Roadside")
        ));

        policies.put("POL-MTR-003", new MotorPolicy(
                "POL-MTR-003",
                "MTR-2025-0440",
                "Legacy Holder",
                null,
                "Nissan",
                "Altima",
                2020,
                new BigDecimal("1200.00"),
                "AED",
                LocalDate.parse("2025-01-01"),
                LocalDate.parse("2025-12-31"),
                MotorPolicyStatus.LAPSED,
                List.of("ThirdParty")
        ));
    }

    @Override
    public List<MotorPolicy> findByPolicyIds(Set<String> policyIds) {
        return policyIds.stream()
                .map(policies::get)
                .filter(policy -> policy != null)
                .toList();
    }

    @Override
    public Optional<MotorPolicy> findByPolicyId(String policyId) {
        return Optional.ofNullable(policies.get(policyId));
    }
}
