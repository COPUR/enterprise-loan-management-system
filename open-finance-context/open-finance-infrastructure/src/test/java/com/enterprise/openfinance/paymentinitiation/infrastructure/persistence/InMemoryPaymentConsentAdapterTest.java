package com.enterprise.openfinance.paymentinitiation.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryPaymentConsentAdapterTest {

    private final InMemoryPaymentConsentAdapter adapter = new InMemoryPaymentConsentAdapter();

    @Test
    void shouldReturnSeededConsent() {
        var consent = adapter.findById("CONS-001");

        assertThat(consent).isPresent();
        assertThat(consent.orElseThrow().currency()).isEqualTo("AED");
    }

    @Test
    void shouldReturnEmptyForUnknownConsent() {
        assertThat(adapter.findById("CONS-UNKNOWN")).isEmpty();
    }
}
