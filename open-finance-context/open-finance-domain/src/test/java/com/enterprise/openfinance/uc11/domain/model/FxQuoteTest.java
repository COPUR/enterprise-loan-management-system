package com.enterprise.openfinance.uc11.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxQuoteTest {

    @Test
    void shouldCreateBookAndExpireQuote() {
        FxQuote quote = quote(FxQuoteStatus.QUOTED);

        FxQuote booked = quote.book(Instant.parse("2026-02-09T10:01:00Z"));
        FxQuote expired = quote.expire(Instant.parse("2026-02-09T10:40:00Z"));

        assertThat(booked.status()).isEqualTo(FxQuoteStatus.BOOKED);
        assertThat(expired.status()).isEqualTo(FxQuoteStatus.EXPIRED);
        assertThat(quote.isExpired(Instant.parse("2026-02-09T10:40:00Z"))).isTrue();
        assertThat(quote.belongsTo("TPP-001")).isTrue();
        assertThat(quote.belongsTo("TPP-OTHER")).isFalse();
    }

    @Test
    void shouldReturnSameInstanceWhenAlreadyExpired() {
        FxQuote expired = quote(FxQuoteStatus.EXPIRED);
        FxQuote result = expired.expire(Instant.parse("2026-02-09T10:40:00Z"));
        assertThat(result).isSameAs(expired);
    }

    @Test
    void shouldRejectInvalidQuote() {
        assertThatThrownBy(() -> new FxQuote(
                " ",
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
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new FxQuote(
                "Q-1",
                "TPP-001",
                "AE",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxQuoteStatus.QUOTED,
                Instant.parse("2026-02-09T10:30:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new FxQuote(
                "Q-1",
                "TPP-001",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                null,
                Instant.parse("2026-02-09T10:30:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static FxQuote quote(FxQuoteStatus status) {
        return new FxQuote(
                "Q-1",
                "TPP-001",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                status,
                Instant.parse("2026-02-09T10:30:00Z"),
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
