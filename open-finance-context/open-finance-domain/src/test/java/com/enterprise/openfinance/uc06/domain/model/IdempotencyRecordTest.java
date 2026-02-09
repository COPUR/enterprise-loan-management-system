package com.enterprise.openfinance.uc06.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyRecordTest {

    @Test
    void shouldCreateAndEvaluateExpiry() {
        Instant created = Instant.parse("2026-02-09T10:00:00Z");
        IdempotencyRecord record = new IdempotencyRecord(
                "IDEMP-001",
                "TPP-001",
                "hash",
                new PaymentResult("PAY-001", "CONS-001", PaymentStatus.PENDING, "ix-001", created, false),
                created,
                Instant.parse("2026-02-09T11:00:00Z")
        );

        assertThat(record.isExpired(Instant.parse("2026-02-09T10:30:00Z"))).isFalse();
        assertThat(record.isExpired(Instant.parse("2026-02-09T11:30:00Z"))).isTrue();
    }

    @Test
    void shouldRejectInvalidIdempotencyRecord() {
        Instant created = Instant.parse("2026-02-09T10:00:00Z");
        assertThatThrownBy(() -> new IdempotencyRecord(
                " ",
                "TPP-001",
                "hash",
                new PaymentResult("PAY-001", "CONS-001", PaymentStatus.PENDING, "ix-001", created, false),
                created,
                Instant.parse("2026-02-09T11:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("idempotencyKey is required");

        assertThatThrownBy(() -> new IdempotencyRecord(
                "IDEMP-001",
                "TPP-001",
                "hash",
                new PaymentResult("PAY-001", "CONS-001", PaymentStatus.PENDING, "ix-001", created, false),
                created,
                created
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("expiresAt must be after createdAt");
    }
}
