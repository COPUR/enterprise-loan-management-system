package com.enterprise.openfinance.insurancedata.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MotorPolicyTest {

    @Test
    void shouldCreatePolicyAndMaskHolder() {
        MotorPolicy policy = new MotorPolicy(
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
        );

        assertThat(policy.holderNameMasked()).startsWith("A").contains("***");
        assertThat(policy.isActive()).isTrue();
    }

    @Test
    void shouldRejectInvalidPolicy() {
        assertThatThrownBy(() -> new MotorPolicy("", "MTR-1", "Ali", null, "Toyota", "Camry", 2023, new BigDecimal("1"), "AED", LocalDate.now(), LocalDate.now().plusDays(1), MotorPolicyStatus.ACTIVE, List.of("Collision")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("policyId");

        assertThatThrownBy(() -> new MotorPolicy("POL-1", "MTR-1", "Ali", null, "Toyota", "Camry", 1800, new BigDecimal("1"), "AED", LocalDate.now(), LocalDate.now().plusDays(1), MotorPolicyStatus.ACTIVE, List.of("Collision")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vehicleYear");

        assertThatThrownBy(() -> new MotorPolicy("POL-1", "MTR-1", "Ali", null, "Toyota", "Camry", 2023, new BigDecimal("0"), "AED", LocalDate.now(), LocalDate.now().plusDays(1), MotorPolicyStatus.ACTIVE, List.of("Collision")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("premiumAmount");

        assertThatThrownBy(() -> new MotorPolicy("POL-1", "MTR-1", "Ali", null, "Toyota", "Camry", 2023, new BigDecimal("1"), "AED", LocalDate.parse("2026-12-31"), LocalDate.parse("2026-01-01"), MotorPolicyStatus.ACTIVE, List.of("Collision")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endDate");
    }
}
