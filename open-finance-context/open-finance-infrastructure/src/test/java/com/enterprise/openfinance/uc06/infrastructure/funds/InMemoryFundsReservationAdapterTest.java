package com.enterprise.openfinance.uc06.infrastructure.funds;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryFundsReservationAdapterTest {

    @Test
    void shouldReserveFundsWhenBalanceIsSufficient() {
        InMemoryFundsReservationAdapter adapter = new InMemoryFundsReservationAdapter();

        boolean reserved = adapter.reserve("ACC-DEBTOR-001", new BigDecimal("100.00"), "AED", "RES-001");

        assertThat(reserved).isTrue();
    }

    @Test
    void shouldRejectReservationWhenBalanceIsInsufficient() {
        InMemoryFundsReservationAdapter adapter = new InMemoryFundsReservationAdapter();

        boolean reserved = adapter.reserve("ACC-LOW-001", new BigDecimal("100.00"), "AED", "RES-001");

        assertThat(reserved).isFalse();
    }
}
