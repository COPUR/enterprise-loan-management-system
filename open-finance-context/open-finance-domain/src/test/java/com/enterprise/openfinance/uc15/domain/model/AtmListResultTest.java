package com.enterprise.openfinance.uc15.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtmListResultTest {

    @Test
    void shouldCreateListResult() {
        AtmLocation atm = new AtmLocation(
                "ATM-001",
                "Downtown ATM",
                AtmStatus.IN_SERVICE,
                25.2048,
                55.2708,
                "Sheikh Zayed Road",
                "Dubai",
                "AE",
                "Full",
                List.of("CashWithdrawal"),
                Instant.parse("2026-02-10T00:00:00Z")
        );

        AtmListResult result = new AtmListResult(List.of(atm), false);

        assertThat(result.atms()).hasSize(1);
        assertThat(result.cacheHit()).isFalse();
    }

    @Test
    void shouldRejectNullList() {
        assertThatThrownBy(() -> new AtmListResult(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("atms");
    }
}
