package com.enterprise.openfinance.uc11.infrastructure.event;

import com.enterprise.openfinance.uc11.domain.model.FxDeal;
import com.enterprise.openfinance.uc11.domain.model.FxDealStatus;
import com.enterprise.openfinance.uc11.domain.model.FxQuote;
import com.enterprise.openfinance.uc11.domain.model.FxQuoteStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;

@Tag("unit")
class NoOpFxEventAdapterTest {

    @Test
    void shouldPublishWithoutThrowing() {
        NoOpFxEventAdapter adapter = new NoOpFxEventAdapter();

        assertThatCode(() -> adapter.publishQuoteCreated(quote())).doesNotThrowAnyException();
        assertThatCode(() -> adapter.publishDealBooked(deal())).doesNotThrowAnyException();
    }

    private static FxQuote quote() {
        return new FxQuote(
                "Q-1",
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

    private static FxDeal deal() {
        return new FxDeal(
                "DEAL-1",
                "Q-1",
                "TPP-1",
                "IDEMP-1",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxDealStatus.BOOKED,
                Instant.parse("2026-02-09T10:00:00Z")
        );
    }
}
