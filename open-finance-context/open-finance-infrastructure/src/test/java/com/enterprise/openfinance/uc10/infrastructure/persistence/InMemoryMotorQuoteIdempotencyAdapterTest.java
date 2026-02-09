package com.enterprise.openfinance.uc10.infrastructure.persistence;

import com.enterprise.openfinance.uc10.domain.model.MotorQuoteIdempotencyRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryMotorQuoteIdempotencyAdapterTest {

    @Test
    void shouldReturnRecordBeforeExpiryAndEvictAfter() {
        InMemoryMotorQuoteIdempotencyAdapter adapter = new InMemoryMotorQuoteIdempotencyAdapter(2);
        MotorQuoteIdempotencyRecord record = new MotorQuoteIdempotencyRecord(
                "IDEMP-1", "TPP-001", "hash", "Q-1", "POL-1", Instant.parse("2026-02-09T10:10:00Z")
        );
        adapter.save(record);

        assertThat(adapter.find("IDEMP-1", "TPP-001", Instant.parse("2026-02-09T10:01:00Z"))).isPresent();
        assertThat(adapter.find("IDEMP-1", "TPP-001", Instant.parse("2026-02-09T10:11:00Z"))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryMotorQuoteIdempotencyAdapter adapter = new InMemoryMotorQuoteIdempotencyAdapter(1);
        adapter.save(new MotorQuoteIdempotencyRecord(
                "IDEMP-1", "TPP-001", "hash1", "Q-1", "POL-1", Instant.parse("2026-02-09T10:10:00Z")
        ));
        adapter.save(new MotorQuoteIdempotencyRecord(
                "IDEMP-2", "TPP-001", "hash2", "Q-2", "POL-2", Instant.parse("2026-02-09T10:10:00Z")
        ));

        assertThat(adapter.find("IDEMP-1", "TPP-001", Instant.parse("2026-02-09T10:01:00Z"))).isEmpty();
        assertThat(adapter.find("IDEMP-2", "TPP-001", Instant.parse("2026-02-09T10:01:00Z"))).isPresent();
    }
}
