package com.enterprise.openfinance.fxservices.infrastructure.persistence;

import com.enterprise.openfinance.fxservices.domain.model.FxDeal;
import com.enterprise.openfinance.fxservices.domain.model.FxDealStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryFxDealAdapterTest {

    @Test
    void shouldSaveAndFindDeal() {
        InMemoryFxDealAdapter adapter = new InMemoryFxDealAdapter();
        FxDeal deal = deal("DEAL-1");

        adapter.save(deal);

        assertThat(adapter.findById("DEAL-1")).contains(deal);
        assertThat(adapter.findById("DEAL-404")).isEmpty();
    }

    private static FxDeal deal(String id) {
        return new FxDeal(
                id,
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
