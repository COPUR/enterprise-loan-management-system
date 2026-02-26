package com.enterprise.openfinance.insurancequotes.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class IssuedPolicyTest {

    @Test
    void shouldCreateAndNormalizeIssuedPolicy() {
        IssuedPolicy policy = new IssuedPolicy(" POL-1 ", " POLNO-1 ", " CERT-1 ");

        assertThat(policy.policyId()).isEqualTo("POL-1");
        assertThat(policy.policyNumber()).isEqualTo("POLNO-1");
        assertThat(policy.certificateId()).isEqualTo("CERT-1");
    }

    @Test
    void shouldRejectBlankFields() {
        assertThatThrownBy(() -> new IssuedPolicy(" ", "POLNO-1", "CERT-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IssuedPolicy("POL-1", " ", "CERT-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IssuedPolicy("POL-1", "POLNO-1", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
