package com.enterprise.openfinance.dynamiconboarding.infrastructure.persistence;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingIdempotencyRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryOnboardingIdempotencyAdapterTest {

    @Test
    void shouldStoreLookupAndExpireEntries() {
        InMemoryOnboardingIdempotencyAdapter adapter = new InMemoryOnboardingIdempotencyAdapter(10);
        OnboardingIdempotencyRecord active = new OnboardingIdempotencyRecord(
                "IDEMP-001",
                "TPP-001",
                "hash-1",
                "ACC-001",
                Instant.parse("2026-02-09T10:10:00Z")
        );

        adapter.save(active);

        assertThat(adapter.find("IDEMP-001", "TPP-001", Instant.parse("2026-02-09T10:05:00Z"))).contains(active);
        assertThat(adapter.find("IDEMP-001", "TPP-001", Instant.parse("2026-02-09T10:10:00Z"))).isEmpty();
    }
}
