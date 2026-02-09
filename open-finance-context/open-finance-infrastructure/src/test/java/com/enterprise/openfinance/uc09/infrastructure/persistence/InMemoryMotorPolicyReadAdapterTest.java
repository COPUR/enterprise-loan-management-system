package com.enterprise.openfinance.uc09.infrastructure.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryMotorPolicyReadAdapterTest {

    @Test
    void shouldFindPoliciesByIdAndSet() {
        InMemoryMotorPolicyReadAdapter adapter = new InMemoryMotorPolicyReadAdapter();

        assertThat(adapter.findByPolicyId("POL-MTR-001")).isPresent();
        assertThat(adapter.findByPolicyId("POL-404")).isEmpty();
        assertThat(adapter.findByPolicyIds(Set.of("POL-MTR-001", "POL-MTR-002"))).hasSize(2);
    }
}
