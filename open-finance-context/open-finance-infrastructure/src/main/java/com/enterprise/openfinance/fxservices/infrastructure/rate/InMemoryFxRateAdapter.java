package com.enterprise.openfinance.fxservices.infrastructure.rate;

import com.enterprise.openfinance.fxservices.domain.model.FxRateSnapshot;
import com.enterprise.openfinance.fxservices.domain.port.out.FxRatePort;
import com.enterprise.openfinance.fxservices.infrastructure.config.FxServicesRateProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFxRateAdapter implements FxRatePort {

    private final boolean weekendRatesEnabled;
    private final int rateScale;
    private final Map<String, BigDecimal> rates;

    public InMemoryFxRateAdapter(FxServicesRateProperties properties) {
        this.weekendRatesEnabled = properties.isWeekendRatesEnabled();
        this.rateScale = Math.max(1, properties.getRateScale());
        this.rates = new ConcurrentHashMap<>();
        properties.getPairs().forEach((pair, value) -> rates.put(pair.toUpperCase(), value));
    }

    @Override
    public Optional<FxRateSnapshot> findRate(String pair, Instant now) {
        if (!weekendRatesEnabled && isWeekend(now)) {
            return Optional.empty();
        }

        if (pair == null || pair.isBlank()) {
            return Optional.empty();
        }

        String normalizedPair = pair.toUpperCase();
        BigDecimal value = rates.get(normalizedPair);
        if (value == null || value.signum() <= 0) {
            return Optional.empty();
        }

        return Optional.of(new FxRateSnapshot(
                normalizedPair,
                value.setScale(rateScale, RoundingMode.HALF_UP),
                now,
                "STREAM"
        ));
    }

    private static boolean isWeekend(Instant now) {
        DayOfWeek day = now.atZone(ZoneOffset.UTC).getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
