package com.enterprise.openfinance.insurancequotes.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MotorQuoteIdempotencyRecordAdditionalTest {

    @Test
    void shouldRejectAdditionalInvalidFields() {
        assertThatThrownBy(() -> new MotorQuoteIdempotencyRecord(
                "IDEMP-1", " ", "hash", "Q-1", null, Instant.parse("2026-02-10T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MotorQuoteIdempotencyRecord(
                "IDEMP-1", "TPP-001", " ", "Q-1", null, Instant.parse("2026-02-10T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MotorQuoteIdempotencyRecord(
                "IDEMP-1", "TPP-001", "hash", "Q-1", null, null
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
