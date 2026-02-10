package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryCorporateBalanceReadAdapterTest {

    @Test
    void shouldReturnBalancesByMasterAccount() {
        InMemoryCorporateBalanceReadAdapter adapter = new InMemoryCorporateBalanceReadAdapter();

        assertThat(adapter.findByMasterAccountId("ACC-M-001")).isNotEmpty();
        assertThat(adapter.findByMasterAccountId("ACC-UNKNOWN")).isEmpty();
    }
}
