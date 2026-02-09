package com.enterprise.openfinance.uc09.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryInsuranceConsentAdapterTest {

    @Test
    void shouldReturnSeededConsents() {
        InMemoryInsuranceConsentAdapter adapter = new InMemoryInsuranceConsentAdapter();

        assertThat(adapter.findById("CONS-INS-001")).isPresent();
        assertThat(adapter.findById("CONS-INS-EXPIRED")).isPresent();
        assertThat(adapter.findById("CONS-UNKNOWN")).isEmpty();
    }
}
