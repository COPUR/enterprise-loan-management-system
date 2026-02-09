package com.enterprise.openfinance.uc11.infrastructure.persistence;

import com.enterprise.openfinance.uc11.domain.model.FxIdempotencyRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryFxIdempotencyAdapterTest {

    @Test
    void shouldReturnRecordBeforeExpiryAndEvictAfter() {
        InMemoryFxIdempotencyAdapter adapter = new InMemoryFxIdempotencyAdapter(2);
        adapter.save(new FxIdempotencyRecord("IDEMP-1", "TPP-1", "hash", "DEAL-1", Instant.parse("2026-02-09T10:10:00Z")));

        assertThat(adapter.find("IDEMP-1", "TPP-1", Instant.parse("2026-02-09T10:01:00Z"))).isPresent();
        assertThat(adapter.find("IDEMP-1", "TPP-1", Instant.parse("2026-02-09T10:11:00Z"))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryFxIdempotencyAdapter adapter = new InMemoryFxIdempotencyAdapter(1);
        adapter.save(new FxIdempotencyRecord("IDEMP-1", "TPP-1", "hash1", "DEAL-1", Instant.parse("2026-02-09T10:10:00Z")));
        adapter.save(new FxIdempotencyRecord("IDEMP-2", "TPP-1", "hash2", "DEAL-2", Instant.parse("2026-02-09T10:10:00Z")));

        assertThat(adapter.find("IDEMP-1", "TPP-1", Instant.parse("2026-02-09T10:05:00Z"))).isEmpty();
        assertThat(adapter.find("IDEMP-2", "TPP-1", Instant.parse("2026-02-09T10:05:00Z"))).isPresent();
    }
}
