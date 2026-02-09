package com.enterprise.openfinance.uc04.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryTransactionMetadataReadAdapterTest {

    @Test
    void shouldReturnTransactionsForKnownAccount() {
        InMemoryTransactionMetadataReadAdapter adapter = new InMemoryTransactionMetadataReadAdapter();

        assertThat(adapter.findByAccountId("ACC-001")).isNotEmpty();
        assertThat(adapter.findByAccountId("ACC-UNKNOWN")).isEmpty();
    }
}
