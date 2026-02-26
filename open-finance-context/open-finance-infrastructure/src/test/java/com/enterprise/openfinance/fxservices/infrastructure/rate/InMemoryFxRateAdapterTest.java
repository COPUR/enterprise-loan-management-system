package com.enterprise.openfinance.fxservices.infrastructure.rate;

import com.enterprise.openfinance.fxservices.infrastructure.config.FxServicesRateProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryFxRateAdapterTest {

    @Test
    void shouldReturnRateWhenMarketOpen() {
        FxServicesRateProperties properties = new FxServicesRateProperties();
        InMemoryFxRateAdapter adapter = new InMemoryFxRateAdapter(properties);

        assertThat(adapter.findRate("AED-USD", Instant.parse("2026-02-09T10:00:00Z"))).isPresent();
    }

    @Test
    void shouldReturnEmptyWhenMarketClosedOnSundayAndNoWeekendRates() {
        FxServicesRateProperties properties = new FxServicesRateProperties();
        properties.setWeekendRatesEnabled(false);
        InMemoryFxRateAdapter adapter = new InMemoryFxRateAdapter(properties);

        assertThat(adapter.findRate("AED-USD", Instant.parse("2026-02-08T10:00:00Z"))).isEmpty();
    }
}
