package com.enterprise.openfinance.bankingmetadata.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryPartyMetadataReadAdapterTest {

    @Test
    void shouldReturnPartiesForKnownAccount() {
        InMemoryPartyMetadataReadAdapter adapter = new InMemoryPartyMetadataReadAdapter();

        assertThat(adapter.findByAccountId("ACC-001")).isNotEmpty();
        assertThat(adapter.findByAccountId("ACC-UNKNOWN")).isEmpty();
    }
}
