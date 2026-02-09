package com.enterprise.openfinance.uc04.domain.query;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetTransactionMetadataQueryTest {

    @Test
    void shouldResolveDefaultsAndBounds() {
        GetTransactionMetadataQuery query = new GetTransactionMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-1",
                null,
                null,
                null,
                500
        );

        assertThat(query.resolvePage()).isEqualTo(1);
        assertThat(query.resolvePageSize(100, 250)).isEqualTo(250);
    }

    @Test
    void shouldRejectInvalidQuery() {
        assertThatThrownBy(() -> new GetTransactionMetadataQuery(
                "",
                "TPP-001",
                "ACC-001",
                "ix-1",
                null,
                null,
                1,
                100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("consentId");

        assertThatThrownBy(() -> new GetTransactionMetadataQuery(
                "CONS-META-001",
                "TPP-001",
                "ACC-001",
                "ix-1",
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                1,
                100
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("toBookingDateTime");
    }
}
