package com.enterprise.openfinance.accountinformation.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryAccountReadAdapterTest {

    private final InMemoryAccountReadAdapter adapter = new InMemoryAccountReadAdapter();

    @Test
    void shouldFindAccountsByPsu() {
        assertThat(adapter.findByPsuId("PSU-001")).hasSizeGreaterThanOrEqualTo(2);
        assertThat(adapter.findByPsuId("PSU-UNKNOWN")).isEmpty();
    }

    @Test
    void shouldFindByAccountId() {
        assertThat(adapter.findById("ACC-001")).isPresent();
        assertThat(adapter.findById("ACC-UNKNOWN")).isEmpty();
    }
}
