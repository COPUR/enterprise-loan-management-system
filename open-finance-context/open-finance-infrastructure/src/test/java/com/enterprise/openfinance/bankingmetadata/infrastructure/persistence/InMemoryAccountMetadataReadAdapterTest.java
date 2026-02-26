package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryAccountMetadataReadAdapterTest {

    @Test
    void shouldReturnAccountMetadataByAccountId() {
        InMemoryAccountMetadataReadAdapter adapter = new InMemoryAccountMetadataReadAdapter();

        assertThat(adapter.findByAccountId("ACC-001")).isPresent();
        assertThat(adapter.findByAccountId("ACC-UNKNOWN")).isEmpty();
    }
}
