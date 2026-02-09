package com.enterprise.openfinance.uc07.infrastructure.cache;

import com.enterprise.openfinance.uc07.domain.model.VrpIdempotencyRecord;
import com.enterprise.openfinance.uc07.domain.model.VrpPaymentStatus;
import com.enterprise.openfinance.uc07.infrastructure.config.Uc07CacheProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryVrpIdempotencyAdapterTest {

    @Test
    void shouldReturnRecordBeforeExpiryAndEvictAfter() {
        InMemoryVrpIdempotencyAdapter adapter = new InMemoryVrpIdempotencyAdapter(properties(10));

        adapter.save(new VrpIdempotencyRecord(
                "IDEMP-001",
                "TPP-001",
                "hash-a",
                "PAY-001",
                VrpPaymentStatus.ACCEPTED,
                Instant.parse("2026-02-09T10:00:30Z")
        ));

        assertThat(adapter.find("IDEMP-001", "TPP-001", Instant.parse("2026-02-09T10:00:01Z"))).isPresent();
        assertThat(adapter.find("IDEMP-001", "TPP-001", Instant.parse("2026-02-09T10:01:01Z"))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryVrpIdempotencyAdapter adapter = new InMemoryVrpIdempotencyAdapter(properties(1));

        adapter.save(new VrpIdempotencyRecord(
                "IDEMP-001",
                "TPP-001",
                "hash-a",
                "PAY-001",
                VrpPaymentStatus.ACCEPTED,
                Instant.parse("2026-02-10T00:00:00Z")
        ));

        adapter.save(new VrpIdempotencyRecord(
                "IDEMP-002",
                "TPP-001",
                "hash-b",
                "PAY-002",
                VrpPaymentStatus.ACCEPTED,
                Instant.parse("2026-02-10T00:00:00Z")
        ));

        assertThat(adapter.find("IDEMP-002", "TPP-001", Instant.parse("2026-02-09T10:00:00Z"))).isPresent();
    }

    private static Uc07CacheProperties properties(int maxEntries) {
        Uc07CacheProperties properties = new Uc07CacheProperties();
        properties.setTtl(Duration.ofSeconds(30));
        properties.setMaxEntries(maxEntries);
        return properties;
    }
}
