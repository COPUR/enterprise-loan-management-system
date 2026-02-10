package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryCorporateTransactionReadAdapterTest {

    @Test
    void shouldReturnTransactionsForKnownAccounts() {
        InMemoryCorporateTransactionReadAdapter adapter = new InMemoryCorporateTransactionReadAdapter();

        assertThat(adapter.findByAccountIds(Set.of("ACC-M-001"))).isNotEmpty();
        assertThat(adapter.findByAccountIds(Set.of("ACC-UNKNOWN"))).isEmpty();
    }
}
