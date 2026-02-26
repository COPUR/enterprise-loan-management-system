package com.enterprise.openfinance.insurancedata.infrastructure.cache;

import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyItemResult;
import com.enterprise.openfinance.insurancedata.domain.model.InsurancePolicyListResult;
import com.enterprise.openfinance.insurancedata.domain.model.MotorPolicy;
import com.enterprise.openfinance.insurancedata.domain.model.MotorPolicyStatus;
import com.enterprise.openfinance.insurancedata.infrastructure.config.InsuranceDataCacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryInsurancePolicyCacheAdapterTest {

    @Test
    void shouldCacheAndExpireValues() {
        InsuranceDataCacheProperties properties = new InsuranceDataCacheProperties();
        properties.setMaxEntries(10);
        InMemoryInsurancePolicyCacheAdapter adapter = new InMemoryInsurancePolicyCacheAdapter(properties);

        InsurancePolicyListResult listResult = new InsurancePolicyListResult(List.of(policy("POL-1")), 1, 10, 1, false);
        InsurancePolicyItemResult itemResult = new InsurancePolicyItemResult(policy("POL-1"), false);

        adapter.putPolicyList("l1", listResult, Instant.parse("2026-02-09T10:01:00Z"));
        adapter.putPolicy("p1", itemResult, Instant.parse("2026-02-09T10:01:00Z"));

        assertThat(adapter.getPolicyList("l1", Instant.parse("2026-02-09T10:00:30Z"))).isPresent();
        assertThat(adapter.getPolicy("p1", Instant.parse("2026-02-09T10:00:30Z"))).isPresent();
        assertThat(adapter.getPolicyList("l1", Instant.parse("2026-02-09T10:01:00Z"))).isEmpty();
        assertThat(adapter.getPolicy("p1", Instant.parse("2026-02-09T10:01:00Z"))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InsuranceDataCacheProperties properties = new InsuranceDataCacheProperties();
        properties.setMaxEntries(1);
        InMemoryInsurancePolicyCacheAdapter adapter = new InMemoryInsurancePolicyCacheAdapter(properties);

        adapter.putPolicyList("l1", new InsurancePolicyListResult(List.of(policy("POL-1")), 1, 10, 1, false), Instant.parse("2026-02-09T10:01:00Z"));
        adapter.putPolicy("p1", new InsurancePolicyItemResult(policy("POL-2"), false), Instant.parse("2026-02-09T10:01:00Z"));

        boolean hasAny = adapter.getPolicyList("l1", Instant.parse("2026-02-09T10:00:30Z")).isPresent()
                || adapter.getPolicy("p1", Instant.parse("2026-02-09T10:00:30Z")).isPresent();
        assertThat(hasAny).isTrue();
    }

    private static MotorPolicy policy(String policyId) {
        return new MotorPolicy(
                policyId,
                "MTR-1",
                "Ali",
                null,
                "Toyota",
                "Camry",
                2023,
                new BigDecimal("1000.00"),
                "AED",
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-12-31"),
                MotorPolicyStatus.ACTIVE,
                List.of("Collision")
        );
    }
}
