package com.enterprise.openfinance.uc05.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryCorporateConsentAdapterTest {

    @Test
    void shouldReturnSeededConsents() {
        InMemoryCorporateConsentAdapter adapter = new InMemoryCorporateConsentAdapter();

        assertThat(adapter.findById("CONS-TRSY-001")).isPresent();
        assertThat(adapter.findById("CONS-TRSY-EXPIRED")).isPresent();
        assertThat(adapter.findById("CONS-UNKNOWN")).isEmpty();
    }
}
