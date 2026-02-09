package com.enterprise.openfinance.uc11.infrastructure.rate;

import com.enterprise.openfinance.uc11.infrastructure.config.Uc11RateProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryFxRateAdapterTest {

    @Test
    void shouldReturnRateWhenMarketOpen() {
        Uc11RateProperties properties = new Uc11RateProperties();
        InMemoryFxRateAdapter adapter = new InMemoryFxRateAdapter(properties);

        assertThat(adapter.findRate("AED-USD", Instant.parse("2026-02-09T10:00:00Z"))).isPresent();
    }

    @Test
    void shouldReturnEmptyWhenMarketClosedOnSundayAndNoWeekendRates() {
        Uc11RateProperties properties = new Uc11RateProperties();
        properties.setWeekendRatesEnabled(false);
        InMemoryFxRateAdapter adapter = new InMemoryFxRateAdapter(properties);

        assertThat(adapter.findRate("AED-USD", Instant.parse("2026-02-08T10:00:00Z"))).isEmpty();
    }
}
