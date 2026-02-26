package com.enterprise.openfinance.corporatetreasury.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateBalanceSnapshotTest {

    @Test
    void shouldFormatAmounts() {
        CorporateBalanceSnapshot snapshot = new CorporateBalanceSnapshot(
                "ACC-M-001",
                "InterimAvailable",
                new BigDecimal("1234.56"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        );

        assertThat(snapshot.amount()).isEqualByComparingTo("1234.56");
        assertThat(snapshot.formattedAmount()).isEqualTo("1234.56");
    }

    @Test
    void shouldRejectInvalidBalance() {
        assertThatThrownBy(() -> new CorporateBalanceSnapshot(
                "",
                "InterimAvailable",
                new BigDecimal("10.00"),
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new CorporateBalanceSnapshot(
                "ACC-M-001",
                "InterimAvailable",
                null,
                "AED",
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }
}
