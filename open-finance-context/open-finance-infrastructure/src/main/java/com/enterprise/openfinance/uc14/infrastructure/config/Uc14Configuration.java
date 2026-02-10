package com.enterprise.openfinance.uc14.infrastructure.config;

import com.enterprise.openfinance.uc14.domain.model.ProductDataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(Uc14CacheProperties.class)
public class Uc14Configuration {

    @Bean
    public Clock productClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProductDataSettings productDataSettings(Uc14CacheProperties properties) {
        return new ProductDataSettings(properties.getTtl());
    }
}
