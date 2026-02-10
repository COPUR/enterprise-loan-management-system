package com.enterprise.openfinance.businessfinancialdata.domain.query;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCorporateTransactionsQueryTest {

    @Test
    void shouldResolveDefaultsAndBoundedSize() {
        GetCorporateTransactionsQuery query = new GetCorporateTransactionsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-1",
                null,
                null,
                null,
                null,
                999
        );

        assertThat(query.resolvePage()).isEqualTo(1);
        assertThat(query.resolvePageSize(100, 250)).isEqualTo(250);
    }

    @Test
    void shouldRejectInvalidValues() {
        assertThatThrownBy(() -> new GetCorporateTransactionsQuery(
                "CONS-TRSY-001",
                "TPP-001",
                "ix-1",
                null,
                Instant.parse("2026-02-10T00:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                1,
                100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toBookingDateTime");

        assertThatThrownBy(() -> new GetCorporateTransactionsQuery(
                "",
                "TPP-001",
                "ix-1",
                null,
                null,
                null,
                1,
                100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");
    }
}
