package com.enterprise.openfinance.fxservices.infrastructure.config;

import com.enterprise.openfinance.fxservices.domain.model.FxSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({FxServicesCacheProperties.class, FxServicesRateProperties.class})
public class FxServicesConfiguration {

    @Bean
    public Clock fxClock() {
        return Clock.systemUTC();
    }

    @Bean
    public FxSettings fxSettings(FxServicesCacheProperties cacheProperties,
                                 FxServicesRateProperties rateProperties) {
        return new FxSettings(
                cacheProperties.getQuoteTtl(),
                cacheProperties.getIdempotencyTtl(),
                cacheProperties.getCacheTtl(),
                rateProperties.getRateScale()
        );
    }
}
