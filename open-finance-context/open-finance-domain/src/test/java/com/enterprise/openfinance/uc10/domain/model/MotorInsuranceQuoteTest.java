package com.enterprise.openfinance.uc10.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MotorInsuranceQuoteTest {

    @Test
    void shouldCreateAcceptAndExpireQuote() {
        MotorInsuranceQuote quote = new MotorInsuranceQuote(
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

        MotorInsuranceQuote accepted = quote.accept(
                "POL-1",
                "POLNO-1",
                "CERT-1",
                "PAY-1",
                Instant.parse("2026-02-09T10:01:00Z")
        );

        MotorInsuranceQuote expired = quote.expire(Instant.parse("2026-02-09T10:31:00Z"));

        assertThat(quote.isExpired(Instant.parse("2026-02-09T10:31:00Z"))).isTrue();
        assertThat(accepted.status()).isEqualTo(QuoteStatus.ACCEPTED);
        assertThat(accepted.policyId()).isEqualTo("POL-1");
        assertThat(expired.status()).isEqualTo(QuoteStatus.EXPIRED);
    }

    @Test
    void shouldRejectInvalidQuote() {
        assertThatThrownBy(() -> new MotorInsuranceQuote(
                " ", "TPP-001", "TOYOTA", "CAMRY", 2023, 35, 10,
                new BigDecimal("1000.00"), "AED", QuoteStatus.QUOTED,
                Instant.parse("2026-02-09T10:30:00Z"), "hash", null, null, null, null,
                Instant.parse("2026-02-09T10:00:00Z"), Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MotorInsuranceQuote(
                "Q-1", "TPP-001", "TOYOTA", "CAMRY", 2023, 35, 10,
                new BigDecimal("0.00"), "AED", QuoteStatus.QUOTED,
                Instant.parse("2026-02-09T10:30:00Z"), "hash", null, null, null, null,
                Instant.parse("2026-02-09T10:00:00Z"), Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
