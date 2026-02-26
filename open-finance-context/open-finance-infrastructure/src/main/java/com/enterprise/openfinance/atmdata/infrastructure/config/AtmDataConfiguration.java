package com.enterprise.openfinance.atmdata.infrastructure.config;

import com.enterprise.openfinance.atmdata.domain.model.AtmDataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AtmDataCacheProperties.class)
public class AtmDataConfiguration {

    @Bean
    public Clock atmDataClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AtmDataSettings atmDataSettings(AtmDataCacheProperties properties) {
        return new AtmDataSettings(properties.getTtl());
    }
}
