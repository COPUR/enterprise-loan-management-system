package com.enterprise.openfinance.fxservices.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxRateSnapshotTest {

    @Test
    void shouldCreateRateSnapshot() {
        FxRateSnapshot rate = new FxRateSnapshot("AED-USD", new BigDecimal("0.272290"), Instant.parse("2026-02-09T10:00:00Z"), "STREAM");

        assertThat(rate.pair()).isEqualTo("AED-USD");
    }

    @Test
    void shouldRejectInvalidRateSnapshot() {
        assertThatThrownBy(() -> new FxRateSnapshot(" ", new BigDecimal("0.1"), Instant.now(), "STREAM"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxRateSnapshot("AED-USD", new BigDecimal("0"), Instant.now(), "STREAM"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
