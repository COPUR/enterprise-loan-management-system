package com.enterprise.openfinance.uc08.infrastructure.config;

import com.enterprise.openfinance.uc08.domain.model.BulkSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc08CacheProperties.class, Uc08PolicyProperties.class, Uc08ProcessingProperties.class})
public class Uc08Configuration {

    @Bean
    public Clock bulkPaymentsClock() {
        return Clock.systemUTC();
    }

    @Bean
    public BulkSettings bulkSettings(Uc08PolicyProperties policyProperties,
                                     Uc08CacheProperties cacheProperties,
                                     Uc08ProcessingProperties processingProperties) {
        return new BulkSettings(
                policyProperties.getIdempotencyTtl(),
                cacheProperties.getTtl(),
                processingProperties.getMaxFileSizeBytes(),
                processingProperties.getStatusPollsToComplete()
        );
    }
}
