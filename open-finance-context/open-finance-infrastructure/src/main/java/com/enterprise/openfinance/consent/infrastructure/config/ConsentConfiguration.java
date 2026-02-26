package com.enterprise.openfinance.consent.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConsentConfiguration {

    @Bean
    public Clock consentClock() {
        return Clock.systemUTC();
    }
}
