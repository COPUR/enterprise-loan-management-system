package com.enterprise.openfinance.uc10.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MotorQuoteIdempotencyRecordTest {

    @Test
    void shouldCreateAndEvaluateActivity() {
        MotorQuoteIdempotencyRecord record = new MotorQuoteIdempotencyRecord(
                "IDEMP-1",
                "TPP-001",
                "request-hash",
                "Q-1",
                "POL-1",
                Instant.parse("2026-02-10T00:00:00Z")
        );

        assertThat(record.isActiveAt(Instant.parse("2026-02-09T10:00:00Z"))).isTrue();
        assertThat(record.isActiveAt(Instant.parse("2026-02-10T01:00:00Z"))).isFalse();
    }

    @Test
    void shouldRejectInvalidRecord() {
        assertThatThrownBy(() -> new MotorQuoteIdempotencyRecord(
                " ", "TPP-001", "request-hash", "Q-1", null, Instant.parse("2026-02-10T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
