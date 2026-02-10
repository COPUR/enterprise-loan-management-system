package com.enterprise.openfinance.uc13.infrastructure.config;

import com.enterprise.openfinance.uc13.domain.model.PayRequestSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties(Uc13CacheProperties.class)
public class Uc13Configuration {

    @Bean
    public Clock payRequestClock() {
        return Clock.systemUTC();
    }

    @Bean
    public PayRequestSettings payRequestSettings(Uc13CacheProperties properties) {
        return new PayRequestSettings(properties.getTtl());
    }

    @Bean
    public Supplier<String> payRequestConsentIdGenerator() {
        return () -> "CONS-RTP-" + UUID.randomUUID();
    }
}
