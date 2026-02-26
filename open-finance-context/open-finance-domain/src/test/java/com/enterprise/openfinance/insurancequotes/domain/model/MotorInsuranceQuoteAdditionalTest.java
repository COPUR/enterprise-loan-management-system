package com.enterprise.openfinance.insurancequotes.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MotorInsuranceQuoteAdditionalTest {

    @Test
    void shouldEvaluateOwnershipAndExpireIdempotently() {
        MotorInsuranceQuote quote = quote(QuoteStatus.QUOTED);

        assertThat(quote.belongsTo("TPP-001")).isTrue();
        assertThat(quote.belongsTo("TPP-OTHER")).isFalse();

        MotorInsuranceQuote firstExpire = quote.expire(Instant.parse("2026-02-09T10:40:00Z"));
        MotorInsuranceQuote secondExpire = firstExpire.expire(Instant.parse("2026-02-09T11:00:00Z"));

        assertThat(firstExpire.status()).isEqualTo(QuoteStatus.EXPIRED);
        assertThat(secondExpire).isEqualTo(firstExpire);
    }

    @Test
    void shouldRejectAdditionalInvalidFields() {
        assertThatThrownBy(() -> new MotorInsuranceQuote(
                "Q-1",
                "TPP-001",
                "TOYOTA",
                "CAMRY",
                2023,
                120,
                10,
                new BigDecimal("1000.00"),
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
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MotorInsuranceQuote(
                "Q-1",
                "TPP-001",
                "TOYOTA",
                "CAMRY",
                2023,
                35,
                -1,
                new BigDecimal("1000.00"),
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
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MotorInsuranceQuote(
                "Q-1",
                "TPP-001",
                "TOYOTA",
                "CAMRY",
                2023,
                35,
                10,
                new BigDecimal("1000.00"),
                "AED",
                null,
                Instant.parse("2026-02-09T10:30:00Z"),
                "risk-hash",
                null,
                null,
                null,
                null,
                Instant.parse("2026-02-09T10:00:00Z"),
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private static MotorInsuranceQuote quote(QuoteStatus status) {
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
                status,
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
