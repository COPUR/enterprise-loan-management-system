package com.enterprise.openfinance.uc09.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetMotorPoliciesQueryTest {

    @Test
    void shouldResolveDefaultsAndBounds() {
        GetMotorPoliciesQuery query = new GetMotorPoliciesQuery(
                "CONS-INS-001",
                "TPP-001",
                "ix-1",
                null,
                10_000
        );

        assertThat(query.resolvePage()).isEqualTo(1);
        assertThat(query.resolvePageSize(50, 200)).isEqualTo(200);
    }

    @Test
    void shouldRejectInvalidFields() {
        assertThatThrownBy(() -> new GetMotorPoliciesQuery("", "TPP-001", "ix-1", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetMotorPoliciesQuery("CONS-INS-001", "", "ix-1", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetMotorPoliciesQuery("CONS-INS-001", "TPP-001", "", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");

        assertThatThrownBy(() -> new GetMotorPoliciesQuery("CONS-INS-001", "TPP-001", "ix-1", 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");

        assertThatThrownBy(() -> new GetMotorPoliciesQuery("CONS-INS-001", "TPP-001", "ix-1", 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");
    }
}
