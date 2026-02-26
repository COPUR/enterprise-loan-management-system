package com.enterprise.openfinance.insurancequotes.infrastructure.persistence;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorInsuranceQuote;
import com.enterprise.openfinance.insurancequotes.domain.model.QuoteStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryMotorQuoteAdapterTest {

    @Test
    void shouldSaveAndFindQuote() {
        InMemoryMotorQuoteAdapter adapter = new InMemoryMotorQuoteAdapter();
        MotorInsuranceQuote quote = sampleQuote("Q-1");

        adapter.save(quote);

        assertThat(adapter.findById("Q-1")).contains(quote);
        assertThat(adapter.findById("Q-404")).isEmpty();
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
