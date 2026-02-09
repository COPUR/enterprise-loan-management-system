package com.enterprise.openfinance.uc05.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateConsentContextTest {

    @Test
    void shouldNormalizeScopesAndValidateAccountAccess() {
        CorporateConsentContext consent = new CorporateConsentContext(
                "CONS-TRSY-001",
                "TPP-001",
                "CORP-001",
                "restricted",
                Set.of("ReadAccounts", "READ_BALANCES", "read-transactions"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        );

        assertThat(consent.hasScope("READACCOUNTS")).isTrue();
        assertThat(consent.hasScope("ReadBalances")).isTrue();
        assertThat(consent.hasScope("ReadTransactions")).isTrue();
        assertThat(consent.allowsAccount("ACC-M-001")).isTrue();
        assertThat(consent.allowsAccount("ACC-UNKNOWN")).isFalse();
        assertThat(consent.belongsToTpp("TPP-001")).isTrue();
        assertThat(consent.isActive(Instant.parse("2026-01-01T00:00:00Z"))).isTrue();
        assertThat(consent.isRestricted()).isTrue();
    }

    @Test
    void shouldRejectInvalidConstruction() {
        assertThatThrownBy(() -> new CorporateConsentContext(
                "",
                "TPP-001",
                "CORP-001",
                "FULL",
                Set.of("READACCOUNTS"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new CorporateConsentContext(
                "CONS-TRSY-001",
                "TPP-001",
                "CORP-001",
                "",
                Set.of("READACCOUNTS"),
                Set.of("ACC-M-001"),
                Instant.parse("2099-01-01T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entitlement");
    }
}
