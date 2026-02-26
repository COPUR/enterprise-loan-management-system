package com.enterprise.openfinance.bankingmetadata.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetadataConsentContextTest {

    @Test
    void shouldNormalizeScopesAndValidateAccountAccess() {
        MetadataConsentContext consent = new MetadataConsentContext(
                "CONS-META-001",
                "TPP-001",
                Set.of("ReadTransactions", "READ PARTIES", "read_standing-orders"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );

        assertThat(consent.hasScope("READTRANSACTIONS")).isTrue();
        assertThat(consent.hasScope("ReadParties")).isTrue();
        assertThat(consent.hasScope("readStandingOrders")).isTrue();
        assertThat(consent.allowsAccount("ACC-001")).isTrue();
        assertThat(consent.allowsAccount("ACC-999")).isFalse();
        assertThat(consent.isActive(Instant.parse("2026-01-01T00:00:00Z"))).isTrue();
    }

    @Test
    void shouldRejectInvalidConstruction() {
        assertThatThrownBy(() -> new MetadataConsentContext(
                "",
                "TPP-001",
                Set.of("READTRANSACTIONS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new MetadataConsentContext(
                "CONS-META-001",
                "",
                Set.of("READTRANSACTIONS"),
                Set.of("ACC-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");
    }
}
