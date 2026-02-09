package com.enterprise.openfinance.uc09.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsurancePolicyItemResultTest {

    @Test
    void shouldExposePolicyAndCacheFlag() {
        MotorPolicy policy = new MotorPolicy(
                "POL-1", "MTR-1", "Ali", "A***", "Toyota", "Camry", 2023,
                new BigDecimal("1000.00"), "AED", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"),
                MotorPolicyStatus.ACTIVE, List.of("Collision")
        );

        InsurancePolicyItemResult result = new InsurancePolicyItemResult(policy, true);
        assertThat(result.policy().policyId()).isEqualTo("POL-1");
        assertThat(result.cacheHit()).isTrue();
    }

    @Test
    void shouldRejectNullPolicy() {
        assertThatThrownBy(() -> new InsurancePolicyItemResult(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("policy");
    }
}
