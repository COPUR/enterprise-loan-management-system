package com.enterprise.openfinance.businessfinancialdata.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateTransactionSnapshotTest {

    @Test
    void shouldIdentifySweepingTransactions() {
        CorporateTransactionSnapshot sweeping = new CorporateTransactionSnapshot(
                "TX-001",
                "ACC-V-101",
                new BigDecimal("2000.00"),
                "AED",
                Instant.parse("2026-02-01T08:00:00Z"),
                "SWEEP",
                "ZBA",
                "Daily sweep"
        );

        CorporateTransactionSnapshot normal = new CorporateTransactionSnapshot(
                "TX-002",
                "ACC-M-001",
                new BigDecimal("100.00"),
                "AED",
                Instant.parse("2026-02-02T08:00:00Z"),
                "BOOK",
                null,
                "Regular payment"
        );

        assertThat(sweeping.isSweeping()).isTrue();
        assertThat(normal.isSweeping()).isFalse();
    }

    @Test
    void shouldRejectInvalidTransaction() {
        assertThatThrownBy(() -> new CorporateTransactionSnapshot(
                "",
                "ACC-M-001",
                new BigDecimal("100.00"),
                "AED",
                Instant.parse("2026-02-02T08:00:00Z"),
                "BOOK",
                null,
                "Regular payment"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");

        assertThatThrownBy(() -> new CorporateTransactionSnapshot(
                "TX-003",
                "ACC-M-001",
                BigDecimal.ZERO,
                "AED",
                Instant.parse("2026-02-02T08:00:00Z"),
                "BOOK",
                null,
                "Regular payment"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }
}
