package com.enterprise.openfinance.atmdata.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryAtmDirectoryAdapterTest {

    @Test
    void shouldReturnSeededAtms() {
        InMemoryAtmDirectoryAdapter adapter = new InMemoryAtmDirectoryAdapter();

        assertThat(adapter.listAtms()).isNotEmpty();
        assertThat(adapter.listAtms()).extracting("atmId").contains("ATM-001");
    }
}
