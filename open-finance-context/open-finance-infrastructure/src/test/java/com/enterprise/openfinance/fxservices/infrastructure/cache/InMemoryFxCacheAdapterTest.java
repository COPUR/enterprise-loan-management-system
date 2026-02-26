package com.enterprise.openfinance.fxservices.infrastructure.cache;

import com.enterprise.openfinance.fxservices.domain.model.FxQuote;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteItemResult;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryFxCacheAdapterTest {

    @Test
    void shouldCacheAndExpireQuote() {
        InMemoryFxCacheAdapter cache = new InMemoryFxCacheAdapter(2);
        cache.putQuote("Q-1:TPP-1", new FxQuoteItemResult(quote("Q-1"), false), Instant.parse("2026-02-09T10:01:00Z"));

        assertThat(cache.getQuote("Q-1:TPP-1", Instant.parse("2026-02-09T10:00:30Z"))).isPresent();
        assertThat(cache.getQuote("Q-1:TPP-1", Instant.parse("2026-02-09T10:02:00Z"))).isEmpty();
    }

    @Test
    void shouldEvictWhenCapacityExceeded() {
        InMemoryFxCacheAdapter cache = new InMemoryFxCacheAdapter(1);
        cache.putQuote("Q-1:TPP-1", new FxQuoteItemResult(quote("Q-1"), false), Instant.parse("2026-02-09T10:10:00Z"));
        cache.putQuote("Q-2:TPP-1", new FxQuoteItemResult(quote("Q-2"), false), Instant.parse("2026-02-09T10:10:00Z"));

        assertThat(cache.getQuote("Q-1:TPP-1", Instant.parse("2026-02-09T10:05:00Z"))).isEmpty();
        assertThat(cache.getQuote("Q-2:TPP-1", Instant.parse("2026-02-09T10:05:00Z"))).isPresent();
    }

    private static FxQuote quote(String id) {
        return new FxQuote(
                id,
                "TPP-1",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxQuoteStatus.QUOTED,
                Instant.parse("2026-02-09T10:30:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
