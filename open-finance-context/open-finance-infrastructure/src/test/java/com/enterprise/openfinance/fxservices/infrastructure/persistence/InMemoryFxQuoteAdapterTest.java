package com.enterprise.openfinance.fxservices.infrastructure.persistence;

import com.enterprise.openfinance.fxservices.domain.model.FxQuote;
import com.enterprise.openfinance.fxservices.domain.model.FxQuoteStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryFxQuoteAdapterTest {

    @Test
    void shouldSaveAndFindQuote() {
        InMemoryFxQuoteAdapter adapter = new InMemoryFxQuoteAdapter();
        FxQuote quote = quote("Q-1");

        adapter.save(quote);

        assertThat(adapter.findById("Q-1")).contains(quote);
        assertThat(adapter.findById("Q-404")).isEmpty();
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
