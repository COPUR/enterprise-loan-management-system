package com.enterprise.openfinance.uc02.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryBalanceReadAdapterTest {

    private final InMemoryBalanceReadAdapter adapter = new InMemoryBalanceReadAdapter();

    @Test
    void shouldReturnBalancesForKnownAccount() {
        assertThat(adapter.findByAccountId("ACC-001")).hasSize(2);
    }

    @Test
    void shouldReturnEmptyForUnknownAccount() {
        assertThat(adapter.findByAccountId("ACC-UNKNOWN")).isEmpty();
    }
}
