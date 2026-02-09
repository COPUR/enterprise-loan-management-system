package com.enterprise.openfinance.uc11.infrastructure.config;

import com.enterprise.openfinance.uc11.domain.model.FxSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc11CacheProperties.class, Uc11RateProperties.class})
public class Uc11Configuration {

    @Bean
    public Clock fxClock() {
        return Clock.systemUTC();
    }

    @Bean
    public FxSettings fxSettings(Uc11CacheProperties cacheProperties,
                                 Uc11RateProperties rateProperties) {
        return new FxSettings(
                cacheProperties.getQuoteTtl(),
                cacheProperties.getIdempotencyTtl(),
                cacheProperties.getCacheTtl(),
                rateProperties.getRateScale()
        );
    }
}
