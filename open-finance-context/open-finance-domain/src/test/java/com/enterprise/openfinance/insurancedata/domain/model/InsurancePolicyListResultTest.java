package com.enterprise.openfinance.insurancedata.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsurancePolicyListResultTest {

    @Test
    void shouldExposePagingMetadata() {
        MotorPolicy policy = new MotorPolicy(
                "POL-1", "MTR-1", "Ali", "A***", "Toyota", "Camry", 2023,
                new BigDecimal("1000.00"), "AED", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"),
                MotorPolicyStatus.ACTIVE, List.of("Collision")
        );

        InsurancePolicyListResult result = new InsurancePolicyListResult(List.of(policy), 1, 10, 12, false);

        assertThat(result.nextPage()).contains(2);
        assertThat(result.cacheHit()).isFalse();
    }

    @Test
    void shouldRejectInvalidResult() {
        assertThatThrownBy(() -> new InsurancePolicyListResult(null, 1, 10, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("policies");

        assertThatThrownBy(() -> new InsurancePolicyListResult(List.of(), 0, 10, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");

        assertThatThrownBy(() -> new InsurancePolicyListResult(List.of(), 1, 0, 0, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");

        assertThatThrownBy(() -> new InsurancePolicyListResult(List.of(), 1, 10, -1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalRecords");
    }
}
