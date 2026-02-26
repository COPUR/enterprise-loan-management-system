package com.enterprise.openfinance.fxservices.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class FxDealTest {

    @Test
    void shouldCreateDealAndCheckOwnership() {
        FxDeal deal = new FxDeal(
                " DEAL-1 ",
                " Q-1 ",
                " TPP-001 ",
                " IDEMP-1 ",
                "aed",
                "usd",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxDealStatus.BOOKED,
                Instant.parse("2026-02-09T10:00:00Z")
        );

        assertThat(deal.dealId()).isEqualTo("DEAL-1");
        assertThat(deal.quoteId()).isEqualTo("Q-1");
        assertThat(deal.tppId()).isEqualTo("TPP-001");
        assertThat(deal.idempotencyKey()).isEqualTo("IDEMP-1");
        assertThat(deal.sourceCurrency()).isEqualTo("AED");
        assertThat(deal.targetCurrency()).isEqualTo("USD");
        assertThat(deal.belongsTo("TPP-001")).isTrue();
        assertThat(deal.belongsTo("TPP-OTHER")).isFalse();
    }

    @Test
    void shouldRejectInvalidDeal() {
        assertThatThrownBy(() -> new FxDeal(
                " ",
                "Q-1",
                "TPP-001",
                "IDEMP-1",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxDealStatus.BOOKED,
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new FxDeal(
                "DEAL-1",
                "Q-1",
                "TPP-001",
                "IDEMP-1",
                "AE",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                FxDealStatus.BOOKED,
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new FxDeal(
                "DEAL-1",
                "Q-1",
                "TPP-001",
                "IDEMP-1",
                "AED",
                "USD",
                new BigDecimal("1000.00"),
                new BigDecimal("272.29"),
                new BigDecimal("0.272290"),
                null,
                Instant.parse("2026-02-09T10:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
