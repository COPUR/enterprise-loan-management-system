package com.enterprise.openfinance.personalfinancialdata.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceSnapshotTest {

    @Test
    void shouldCreateBalanceSnapshot() {
        BalanceSnapshot snapshot = new BalanceSnapshot(
                "ACC-001",
                "InterimAvailable",
                new BigDecimal("100.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        );

        assertThat(snapshot.accountId()).isEqualTo("ACC-001");
        assertThat(snapshot.currency()).isEqualTo("AED");
    }

    @Test
    void shouldRejectInvalidBalanceSnapshot() {
        assertThatThrownBy(() -> new BalanceSnapshot(
                "",
                "InterimAvailable",
                new BigDecimal("100.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new BalanceSnapshot(
                "ACC-001",
                "",
                new BigDecimal("100.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("balanceType");

        assertThatThrownBy(() -> new BalanceSnapshot(
                "ACC-001",
                "InterimAvailable",
                null,
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> new BalanceSnapshot(
                "ACC-001",
                "InterimAvailable",
                new BigDecimal("100.00"),
                "",
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");

        assertThatThrownBy(() -> new BalanceSnapshot(
                "ACC-001",
                "InterimAvailable",
                new BigDecimal("100.00"),
                "AED",
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asOf");
    }
}
