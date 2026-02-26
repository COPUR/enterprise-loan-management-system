package com.enterprise.openfinance.insurancedata.domain.query;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetMotorPolicyQueryTest {

    @Test
    void shouldCreateAndNormalizeQuery() {
        GetMotorPolicyQuery query = new GetMotorPolicyQuery(" CONS-INS-001 ", " TPP-001 ", " POL-MTR-001 ", " ix-1 ");

        assertThat(query.consentId()).isEqualTo("CONS-INS-001");
        assertThat(query.tppId()).isEqualTo("TPP-001");
        assertThat(query.policyId()).isEqualTo("POL-MTR-001");
        assertThat(query.interactionId()).isEqualTo("ix-1");
    }

    @Test
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> new GetMotorPolicyQuery("", "TPP-001", "POL-MTR-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetMotorPolicyQuery("CONS-INS-001", "", "POL-MTR-001", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetMotorPolicyQuery("CONS-INS-001", "TPP-001", "", "ix-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("policyId");

        assertThatThrownBy(() -> new GetMotorPolicyQuery("CONS-INS-001", "TPP-001", "POL-MTR-001", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
