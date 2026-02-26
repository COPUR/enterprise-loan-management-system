package com.enterprise.openfinance.insurancequotes.infrastructure.config;

import com.enterprise.openfinance.insurancequotes.domain.model.QuoteSettings;
import com.enterprise.openfinance.insurancequotes.domain.port.out.QuotePricingPort;
import com.enterprise.openfinance.insurancequotes.infrastructure.pricing.DeterministicQuotePricingAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({InsuranceQuoteCacheProperties.class, InsuranceQuotePricingProperties.class})
public class InsuranceQuoteConfiguration {

    @Bean
    public Clock insuranceQuoteClock() {
        return Clock.systemUTC();
    }

    @Bean
    public QuoteSettings quoteSettings(InsuranceQuoteCacheProperties cacheProperties,
                                       InsuranceQuotePricingProperties pricingProperties) {
        return new QuoteSettings(
                cacheProperties.getQuoteTtl(),
                cacheProperties.getIdempotencyTtl(),
                cacheProperties.getCacheTtl(),
                "AED",
                pricingProperties.getBasePremium()
        );
    }

    @Bean
    public QuotePricingPort quotePricingPort(InsuranceQuotePricingProperties pricingProperties) {
        return new DeterministicQuotePricingAdapter(pricingProperties.getBasePremium());
    }
}
