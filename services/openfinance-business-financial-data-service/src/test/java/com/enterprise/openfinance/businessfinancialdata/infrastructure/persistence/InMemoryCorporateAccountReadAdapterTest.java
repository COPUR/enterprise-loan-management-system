package com.enterprise.openfinance.businessfinancialdata.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryCorporateAccountReadAdapterTest {

    @Test
    void shouldReturnCorporateAccountsAndFindById() {
        InMemoryCorporateAccountReadAdapter adapter = new InMemoryCorporateAccountReadAdapter();

        assertThat(adapter.findByCorporateId("CORP-001")).isNotEmpty();
        assertThat(adapter.findByCorporateId("CORP-UNKNOWN")).isEmpty();
        assertThat(adapter.findById("ACC-M-001")).isPresent();
    }
}
