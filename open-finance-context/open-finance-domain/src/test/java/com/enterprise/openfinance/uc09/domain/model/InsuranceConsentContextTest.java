package com.enterprise.openfinance.uc09.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsuranceConsentContextTest {

    @Test
    void shouldNormalizeScopesAndValidateAccess() {
        InsuranceConsentContext context = new InsuranceConsentContext(
                "CONS-INS-001",
                "TPP-001",
                Set.of("Read-Policies"),
                Set.of("POL-MTR-001", "POL-MTR-002"),
                Instant.parse("2099-01-01T00:00:00Z")
        );

        assertThat(context.hasScope("ReadPolicies")).isTrue();
        assertThat(context.hasScope("read_policies")).isTrue();
        assertThat(context.belongsToTpp("TPP-001")).isTrue();
        assertThat(context.allowsPolicy("POL-MTR-001")).isTrue();
        assertThat(context.isActive(Instant.parse("2026-02-09T00:00:00Z"))).isTrue();
    }

    @Test
    void shouldRejectInvalidConstruction() {
        assertThatThrownBy(() -> new InsuranceConsentContext("", "TPP-001", Set.of("ReadPolicies"), Set.of("POL-1"), Instant.parse("2099-01-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new InsuranceConsentContext("CONS-INS-001", "", Set.of("ReadPolicies"), Set.of("POL-1"), Instant.parse("2099-01-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new InsuranceConsentContext("CONS-INS-001", "TPP-001", Set.of("ReadPolicies"), Set.of(" "), Instant.parse("2099-01-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("policyId");

        assertThatThrownBy(() -> new InsuranceConsentContext("CONS-INS-001", "TPP-001", Set.of("ReadPolicies"), Set.of("POL-1"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt");
    }
}
