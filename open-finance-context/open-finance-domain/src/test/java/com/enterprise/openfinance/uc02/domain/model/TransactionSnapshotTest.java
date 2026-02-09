package com.enterprise.openfinance.uc02.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionSnapshotTest {

    @Test
    void shouldCreateTransactionSnapshot() {
        TransactionSnapshot snapshot = new TransactionSnapshot(
                "TXN-001",
                "ACC-001",
                new BigDecimal("25.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                "Debit",
                "Booked",
                "Merchant"
        );

        assertThat(snapshot.transactionId()).isEqualTo("TXN-001");
        assertThat(snapshot.status()).isEqualTo("Booked");
    }

    @Test
    void shouldRejectInvalidTransactionSnapshot() {
        assertThatThrownBy(() -> new TransactionSnapshot(
                "",
                "ACC-001",
                new BigDecimal("25.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                "Debit",
                "Booked",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactionId");

        assertThatThrownBy(() -> new TransactionSnapshot(
                "TXN-001",
                "",
                new BigDecimal("25.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                "Debit",
                "Booked",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new TransactionSnapshot(
                "TXN-001",
                "ACC-001",
                null,
                "AED",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                "Debit",
                "Booked",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");

        assertThatThrownBy(() -> new TransactionSnapshot(
                "TXN-001",
                "ACC-001",
                new BigDecimal("25.00"),
                "",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                "Debit",
                "Booked",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");

        assertThatThrownBy(() -> new TransactionSnapshot(
                "TXN-001",
                "ACC-001",
                new BigDecimal("25.00"),
                "AED",
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                "Debit",
                "Booked",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bookingDateTime");

        assertThatThrownBy(() -> new TransactionSnapshot(
                "TXN-001",
                "ACC-001",
                new BigDecimal("25.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z"),
                null,
                "Debit",
                "Booked",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valueDateTime");

        assertThatThrownBy(() -> new TransactionSnapshot(
                "TXN-001",
                "ACC-001",
                new BigDecimal("25.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                "",
                "Booked",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creditDebitIndicator");

        assertThatThrownBy(() -> new TransactionSnapshot(
                "TXN-001",
                "ACC-001",
                new BigDecimal("25.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                "Debit",
                "",
                "Merchant"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }
}
