package com.enterprise.openfinance.uc04.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryStandingOrderMetadataReadAdapterTest {

    @Test
    void shouldReturnStandingOrdersByAccountOrAll() {
        InMemoryStandingOrderMetadataReadAdapter adapter = new InMemoryStandingOrderMetadataReadAdapter();

        assertThat(adapter.findByAccountId("ACC-001")).isNotEmpty();
        assertThat(adapter.findAll()).isNotEmpty();
        assertThat(adapter.findByAccountId("ACC-UNKNOWN")).isEmpty();
    }
}
