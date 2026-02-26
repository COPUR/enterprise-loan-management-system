package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryMetadataConsentAdapterTest {

    @Test
    void shouldLoadSeededConsents() {
        InMemoryMetadataConsentAdapter adapter = new InMemoryMetadataConsentAdapter();

        assertThat(adapter.findById("CONS-META-001")).isPresent();
        assertThat(adapter.findById("CONS-META-001").orElseThrow().hasScope("ReadTransactions")).isTrue();
        assertThat(adapter.findById("CONS-META-EXPIRED")).isPresent();
    }
}
