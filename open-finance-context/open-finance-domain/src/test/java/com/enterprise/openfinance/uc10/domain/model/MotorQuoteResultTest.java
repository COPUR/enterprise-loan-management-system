package com.enterprise.openfinance.uc10.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MotorQuoteResultTest {

    @Test
    void shouldExposeResultFlags() {
        MotorInsuranceQuote quote = quote();
        MotorQuoteResult create = new MotorQuoteResult(quote, false);
        MotorQuoteItemResult get = new MotorQuoteItemResult(quote, false);

        assertThat(create.idempotencyReplay()).isFalse();
        assertThat(get.withCacheHit(true).cacheHit()).isTrue();
    }

    @Test
    void shouldRejectNullQuoteInResults() {
        assertThatThrownBy(() -> new MotorQuoteResult(null, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MotorQuoteItemResult(null, false)).isInstanceOf(IllegalArgumentException.class);
    }

    private static MotorInsuranceQuote quote() {
        return new MotorInsuranceQuote(
                "Q-1",
                "TPP-001",
                "TOYOTA",
                "CAMRY",
                2023,
                35,
                10,
                new BigDecimal("1100.00"),
                "AED",
                QuoteStatus.QUOTED,
                Instant.parse("2026-02-09T10:30:00Z"),
                "risk-hash",
                null,
                null,
                null,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
