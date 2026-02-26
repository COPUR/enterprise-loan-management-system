package com.enterprise.openfinance.dynamiconboarding.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class OnboardingIdempotencyRecordTest {

    @Test
    void shouldDetermineActiveWindow() {
        OnboardingIdempotencyRecord record = new OnboardingIdempotencyRecord(
                "IDEMP-001",
                "TPP-001",
                "hash-1",
                "ACC-001",
                Instant.parse("2026-02-10T10:00:00Z")
        );

        assertThat(record.isActiveAt(Instant.parse("2026-02-09T11:00:00Z"))).isTrue();
        assertThat(record.isActiveAt(Instant.parse("2026-02-10T10:00:00Z"))).isFalse();
    }

    @Test
    void shouldRejectInvalidRecord() {
        assertThatThrownBy(() -> new OnboardingIdempotencyRecord(
                " ",
                "TPP-001",
                "hash-1",
                "ACC-001",
                Instant.parse("2026-02-10T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey");
    }
}
