package com.enterprise.openfinance.paymentinitiation.infrastructure.cache;

import com.enterprise.openfinance.paymentinitiation.domain.model.IdempotencyRecord;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentResult;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentStatus;
import com.enterprise.openfinance.paymentinitiation.infrastructure.config.PaymentInitiationIdempotencyProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryPaymentIdempotencyAdapterTest {

    @Test
    void shouldReturnRecordBeforeExpiry() {
        InMemoryPaymentIdempotencyAdapter adapter = new InMemoryPaymentIdempotencyAdapter(properties(10));
        IdempotencyRecord record = record(
                "IDEMP-001",
                "TPP-001",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T11:00:00Z")
        );

        adapter.save(record);

        assertThat(adapter.find("IDEMP-001", "TPP-001", Instant.parse("2026-02-09T10:30:00Z"))).contains(record);
    }

    @Test
    void shouldEvictExpiredRecord() {
        InMemoryPaymentIdempotencyAdapter adapter = new InMemoryPaymentIdempotencyAdapter(properties(10));
        adapter.save(record(
                "IDEMP-001",
                "TPP-001",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:10:00Z")
        ));

        assertThat(adapter.find("IDEMP-001", "TPP-001", Instant.parse("2026-02-09T10:20:00Z"))).isEmpty();
    }

    @Test
    void shouldEvictOneWhenCapacityExceeded() {
        InMemoryPaymentIdempotencyAdapter adapter = new InMemoryPaymentIdempotencyAdapter(properties(1));
        adapter.save(record(
                "IDEMP-001",
                "TPP-001",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T11:00:00Z")
        ));
        adapter.save(record(
                "IDEMP-002",
                "TPP-001",
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T11:00:00Z")
        ));

        assertThat(adapter.find("IDEMP-002", "TPP-001", Instant.parse("2026-02-09T10:10:00Z"))).isPresent();
        assertThat(adapter.find("IDEMP-001", "TPP-001", Instant.parse("2026-02-09T10:10:00Z"))).isEmpty();
    }

    private static PaymentInitiationIdempotencyProperties properties(int maxEntries) {
        PaymentInitiationIdempotencyProperties properties = new PaymentInitiationIdempotencyProperties();
        properties.setTtl(Duration.ofHours(24));
        properties.setMaxEntries(maxEntries);
        return properties;
    }

    private static IdempotencyRecord record(String key, String tppId, Instant createdAt, Instant expiresAt) {
        return new IdempotencyRecord(
                key,
                tppId,
                "hash",
                new PaymentResult("PAY-001", "CONS-001", PaymentStatus.ACCEPTED_SETTLEMENT_IN_PROCESS, "ix-1", createdAt, false),
                createdAt,
                expiresAt
        );
    }
}
