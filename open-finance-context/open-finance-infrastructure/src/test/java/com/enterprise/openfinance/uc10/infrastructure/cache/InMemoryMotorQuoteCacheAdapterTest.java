package com.enterprise.openfinance.uc10.infrastructure.cache;

import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.uc10.domain.model.MotorQuoteItemResult;
import com.enterprise.openfinance.uc10.domain.model.QuoteStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryMotorQuoteCacheAdapterTest {

    @Test
    void shouldCacheAndExpireValues() {
        InMemoryMotorQuoteCacheAdapter cache = new InMemoryMotorQuoteCacheAdapter(2);
        MotorQuoteItemResult value = new MotorQuoteItemResult(sampleQuote("Q-1"), false);

        cache.putQuote("Q-1", value, Instant.parse("2026-02-09T10:01:00Z"));

        assertThat(cache.getQuote("Q-1", Instant.parse("2026-02-09T10:00:30Z"))).isPresent();
        assertThat(cache.getQuote("Q-1", Instant.parse("2026-02-09T10:02:00Z"))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryMotorQuoteCacheAdapter cache = new InMemoryMotorQuoteCacheAdapter(1);

        cache.putQuote("Q-1", new MotorQuoteItemResult(sampleQuote("Q-1"), false), Instant.parse("2026-02-09T10:10:00Z"));
        cache.putQuote("Q-2", new MotorQuoteItemResult(sampleQuote("Q-2"), false), Instant.parse("2026-02-09T10:10:00Z"));

        assertThat(cache.getQuote("Q-1", Instant.parse("2026-02-09T10:05:00Z"))).isEmpty();
        assertThat(cache.getQuote("Q-2", Instant.parse("2026-02-09T10:05:00Z"))).isPresent();
    }

    private static MotorInsuranceQuote sampleQuote(String quoteId) {
        return new MotorInsuranceQuote(
                quoteId,
                "TPP-001",
                "TOYOTA",
                "CAMRY",
                2023,
                35,
                10,
                new BigDecimal("1000.00"),
                "AED",
                QuoteStatus.QUOTED,
                Instant.parse("2026-02-09T10:30:00Z"),
                "hash",
                null,
                null,
                null,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
