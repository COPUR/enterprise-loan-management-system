package com.enterprise.openfinance.uc04.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandingOrderMetadataTest {

    @Test
    void shouldBuildStandingOrderMetadata() {
        StandingOrderMetadata metadata = new StandingOrderMetadata(
                "SO-001",
                "ACC-001",
                "EvryMnth",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"),
                new BigDecimal("500.00"),
                "AED"
        );

        assertThat(metadata.standingOrderId()).isEqualTo("SO-001");
        assertThat(metadata.amount()).isEqualByComparingTo("500.00");
        assertThat(metadata.currency()).isEqualTo("AED");
    }

    @Test
    void shouldRejectInvalidStandingOrderValues() {
        assertThatThrownBy(() -> new StandingOrderMetadata(
                "",
                "ACC-001",
                "EvryMnth",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                new BigDecimal("500.00"),
                "AED"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("standingOrderId");

        assertThatThrownBy(() -> new StandingOrderMetadata(
                "SO-001",
                "ACC-001",
                "EvryMnth",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2025-12-31T00:00:00Z"),
                new BigDecimal("500.00"),
                "AED"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finalPaymentDate");

        assertThatThrownBy(() -> new StandingOrderMetadata(
                "SO-001",
                "ACC-001",
                "EvryMnth",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                new BigDecimal("0.00"),
                "AED"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }
}
