package com.enterprise.openfinance.uc07.infrastructure.config;

import com.enterprise.openfinance.uc07.domain.model.VrpSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc07CacheProperties.class, Uc07PolicyProperties.class})
public class Uc07Configuration {

    @Bean
    public Clock vrpClock() {
        return Clock.systemUTC();
    }

    @Bean
    public VrpSettings vrpSettings(Uc07PolicyProperties policyProperties, Uc07CacheProperties cacheProperties) {
        return new VrpSettings(policyProperties.getIdempotencyTtl(), cacheProperties.getTtl());
    }
}
