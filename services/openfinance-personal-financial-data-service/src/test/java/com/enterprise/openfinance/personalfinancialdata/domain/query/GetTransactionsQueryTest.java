package com.enterprise.openfinance.personalfinancialdata.domain.query;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetTransactionsQueryTest {

    @Test
    void shouldResolveDefaultAndBoundedPaginationValues() {
        GetTransactionsQuery query = new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "ix-001",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"),
                null,
                500
        );

        assertThat(query.resolvePage()).isEqualTo(1);
        assertThat(query.resolvePageSize(100, 100)).isEqualTo(100);
    }

    @Test
    void shouldResolveProvidedPaginationValues() {
        GetTransactionsQuery query = new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "ix-001",
                null,
                null,
                3,
                50
        );

        assertThat(query.resolvePage()).isEqualTo(3);
        assertThat(query.resolvePageSize(100, 100)).isEqualTo(50);
    }

    @Test
    void shouldRejectInvalidDateRangeAndPagination() {
        assertThatThrownBy(() -> new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "ix-001",
                Instant.parse("2026-12-31T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                1,
                100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toBookingDateTime");

        assertThatThrownBy(() -> new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "ix-001",
                null,
                null,
                0,
                100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");

        assertThatThrownBy(() -> new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "ix-001",
                null,
                null,
                1,
                0
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageSize");

        assertThatThrownBy(() -> new GetTransactionsQuery(
                "",
                "TPP-001",
                "ACC-001",
                "ix-001",
                null,
                null,
                1,
                10
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetTransactionsQuery(
                "CONS-AIS-001",
                "",
                "ACC-001",
                "ix-001",
                null,
                null,
                1,
                10
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tppId");

        assertThatThrownBy(() -> new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "",
                "ix-001",
                null,
                null,
                1,
                10
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");

        assertThatThrownBy(() -> new GetTransactionsQuery(
                "CONS-AIS-001",
                "TPP-001",
                "ACC-001",
                "",
                null,
                null,
                1,
                10
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interactionId");
    }
}
