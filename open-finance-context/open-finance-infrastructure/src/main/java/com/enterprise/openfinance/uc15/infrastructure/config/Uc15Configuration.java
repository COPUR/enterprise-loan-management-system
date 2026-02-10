package com.enterprise.openfinance.uc15.infrastructure.config;

import com.enterprise.openfinance.uc15.domain.model.AtmDataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(Uc15CacheProperties.class)
public class Uc15Configuration {

    @Bean
    public Clock atmDataClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AtmDataSettings atmDataSettings(Uc15CacheProperties properties) {
        return new AtmDataSettings(properties.getTtl());
    }
}
