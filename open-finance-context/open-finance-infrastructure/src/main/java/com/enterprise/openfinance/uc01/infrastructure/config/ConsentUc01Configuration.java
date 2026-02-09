package com.enterprise.openfinance.uc01.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ConsentUc01Configuration {

    @Bean
    public Clock consentClock() {
        return Clock.systemUTC();
    }
}
