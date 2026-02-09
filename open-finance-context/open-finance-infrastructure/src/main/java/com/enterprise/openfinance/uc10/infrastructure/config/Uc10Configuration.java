package com.enterprise.openfinance.uc10.infrastructure.config;

import com.enterprise.openfinance.uc10.domain.model.QuoteSettings;
import com.enterprise.openfinance.uc10.domain.port.out.QuotePricingPort;
import com.enterprise.openfinance.uc10.infrastructure.pricing.DeterministicQuotePricingAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc10CacheProperties.class, Uc10PricingProperties.class})
public class Uc10Configuration {

    @Bean
    public Clock insuranceQuoteClock() {
        return Clock.systemUTC();
    }

    @Bean
    public QuoteSettings quoteSettings(Uc10CacheProperties cacheProperties,
                                       Uc10PricingProperties pricingProperties) {
        return new QuoteSettings(
                cacheProperties.getQuoteTtl(),
                cacheProperties.getIdempotencyTtl(),
                cacheProperties.getCacheTtl(),
                "AED",
                pricingProperties.getBasePremium()
        );
    }

    @Bean
    public QuotePricingPort quotePricingPort(Uc10PricingProperties pricingProperties) {
        return new DeterministicQuotePricingAdapter(pricingProperties.getBasePremium());
    }
}
