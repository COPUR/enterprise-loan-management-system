package com.enterprise.openfinance.uc11.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxQuoteResultTest {

    @Test
    void shouldRejectNullQuote() {
        assertThatThrownBy(() -> new FxQuoteResult(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullItem() {
        assertThatThrownBy(() -> new FxQuoteItemResult(null, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullDealResult() {
        assertThatThrownBy(() -> new FxDealResult(null, false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAllowCacheMutation() {
        FxQuote quote = new FxQuote(
                "Q-1",
                "TPP-001",
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
        FxQuoteItemResult result = new FxQuoteItemResult(quote, false);
        result = result.withCacheHit(true);
        if (!result.cacheHit()) {
            throw new IllegalStateException("Expected cache hit");
        }
    }
}
