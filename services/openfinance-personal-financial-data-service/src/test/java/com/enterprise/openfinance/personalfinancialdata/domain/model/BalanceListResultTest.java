package com.enterprise.openfinance.personalfinancialdata.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceListResultTest {

    @Test
    void shouldCreateBalanceListResult() {
        BalanceListResult result = new BalanceListResult(List.of(new BalanceSnapshot(
                "ACC-001",
                "InterimAvailable",
                new BigDecimal("100.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        )), false);

        assertThat(result.balances()).hasSize(1);
        assertThat(result.cacheHit()).isFalse();
    }

    @Test
    void shouldRejectNullBalances() {
        assertThatThrownBy(() -> new BalanceListResult(null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balances");
    }
}
