package com.enterprise.openfinance.accountinformation.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryTransactionReadAdapterTest {

    private final InMemoryTransactionReadAdapter adapter = new InMemoryTransactionReadAdapter();

    @Test
    void shouldReturnLargeDatasetForPrimaryAccount() {
        assertThat(adapter.findByAccountId("ACC-001")).hasSizeGreaterThan(100);
    }

    @Test
    void shouldReturnTransactionsForUsdAccount() {
        assertThat(adapter.findByAccountId("ACC-002"))
                .isNotEmpty()
                .allMatch(tx -> "USD".equals(tx.currency()));
    }

    @Test
    void shouldReturnEmptyForUnknownAccount() {
        assertThat(adapter.findByAccountId("ACC-UNKNOWN")).isEmpty();
    }
}
