package com.enterprise.openfinance.uc10.infrastructure.event;

import com.enterprise.openfinance.uc10.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.uc10.domain.model.QuoteStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

@Tag("unit")
class NoOpMotorQuoteEventAdapterTest {

    @Test
    void shouldPublishWithoutThrowing() {
        NoOpMotorQuoteEventAdapter adapter = new NoOpMotorQuoteEventAdapter();
        MotorInsuranceQuote quote = sampleQuote();

        assertThatCode(() -> adapter.publishQuoteCreated(quote)).doesNotThrowAnyException();
        assertThatCode(() -> adapter.publishQuoteAccepted(quote)).doesNotThrowAnyException();
        assertThatCode(() -> adapter.publishPolicyIssued(quote)).doesNotThrowAnyException();
    }

    private static MotorInsuranceQuote sampleQuote() {
        return new MotorInsuranceQuote(
                "Q-1",
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
