package com.enterprise.openfinance.accountinformation.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryAisConsentAdapterTest {

    private final InMemoryAisConsentAdapter adapter = new InMemoryAisConsentAdapter();

    @Test
    void shouldReturnSeededFullScopeConsent() {
        var consent = adapter.findById("CONS-AIS-001");

        assertThat(consent).isPresent();
        assertThat(consent.orElseThrow().hasScope("ReadAccounts")).isTrue();
        assertThat(consent.orElseThrow().hasScope("ReadBalances")).isTrue();
        assertThat(consent.orElseThrow().hasScope("ReadTransactions")).isTrue();
        assertThat(consent.orElseThrow().isActive(Instant.parse("2026-01-01T00:00:00Z"))).isTrue();
    }

    @Test
    void shouldReturnSeededBalanceOnlyConsent() {
        var consent = adapter.findById("CONS-AIS-BAL-ONLY");

        assertThat(consent).isPresent();
        assertThat(consent.orElseThrow().hasScope("ReadBalances")).isTrue();
        assertThat(consent.orElseThrow().hasScope("ReadTransactions")).isFalse();
    }

    @Test
    void shouldReturnEmptyForUnknownConsent() {
        assertThat(adapter.findById("CONS-UNKNOWN")).isEmpty();
    }
}
