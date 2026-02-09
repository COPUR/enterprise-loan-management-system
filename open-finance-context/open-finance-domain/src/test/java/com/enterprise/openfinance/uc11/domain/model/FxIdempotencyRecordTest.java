package com.enterprise.openfinance.uc11.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxIdempotencyRecordTest {

    @Test
    void shouldEvaluateExpiry() {
        FxIdempotencyRecord record = new FxIdempotencyRecord("IDEMP-1", "TPP-001", "hash", "DEAL-1", Instant.parse("2026-02-10T00:00:00Z"));

        assertThat(record.isActiveAt(Instant.parse("2026-02-09T10:00:00Z"))).isTrue();
        assertThat(record.isActiveAt(Instant.parse("2026-02-10T01:00:00Z"))).isFalse();
    }

    @Test
    void shouldRejectInvalidRecord() {
        assertThatThrownBy(() -> new FxIdempotencyRecord(" ", "TPP-001", "hash", "DEAL-1", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxIdempotencyRecord("IDEMP-1", "TPP-001", " ", "DEAL-1", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FxIdempotencyRecord("IDEMP-1", "TPP-001", "hash", " ", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
