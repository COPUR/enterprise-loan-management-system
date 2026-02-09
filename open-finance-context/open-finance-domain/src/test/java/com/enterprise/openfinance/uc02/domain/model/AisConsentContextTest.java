package com.enterprise.openfinance.uc02.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AisConsentContextTest {

    @Test
    void shouldNormalizeScopesAndAllowAccountAccessWhenActive() {
        AisConsentContext consent = new AisConsentContext(
                "CONS-AIS-001",
                "TPP-001",
                "PSU-001",
                Set.of("ReadAccounts", "READ BALANCES", "read-transactions"),
                Set.of("ACC-001", "ACC-002"),
                Instant.parse("2099-01-01T00:00:00Z")
        );

        assertThat(consent.hasScope("READ_ACCOUNTS")).isTrue();
        assertThat(consent.hasScope("readBalances")).isTrue();
        assertThat(consent.hasScope("ReadTransactions")).isTrue();
        assertThat(consent.allowsAccount("ACC-001")).isTrue();
        assertThat(consent.allowsAccount("ACC-999")).isFalse();
        assertThat(consent.isActive(Instant.parse("2026-01-01T00:00:00Z"))).isTrue();
    }

    @Test
    void shouldRejectExpiredConsentForActiveEvaluation() {
        AisConsentContext consent = new AisConsentContext(
                "CONS-AIS-EXP",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2025-01-01T00:00:00Z")
        );

        assertThat(consent.isActive(Instant.parse("2026-01-01T00:00:00Z"))).isFalse();
    }

    @Test
    void shouldRejectInvalidConsentConstruction() {
        assertThatThrownBy(() -> new AisConsentContext(
                " ",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new AisConsentContext(
                "CONS-1",
                " ",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new AisConsentContext(
                "CONS-1",
                "TPP-001",
                " ",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("psuId");

        assertThatThrownBy(() -> new AisConsentContext(
                "CONS-1",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of(" "),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new AisConsentContext(
                "CONS-1",
                "TPP-001",
                "PSU-001",
                Set.of(" "),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scope");

        assertThatThrownBy(() -> new AisConsentContext(
                "CONS-1",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiresAt");
    }

    @Test
    void shouldValidateParticipantOwnership() {
        AisConsentContext consent = new AisConsentContext(
                "CONS-AIS-001",
                "TPP-001",
                "PSU-001",
                Set.of("READACCOUNTS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );

        assertThat(consent.belongsToTpp("TPP-001")).isTrue();
        assertThat(consent.belongsToTpp("TPP-999")).isFalse();
    }
}
