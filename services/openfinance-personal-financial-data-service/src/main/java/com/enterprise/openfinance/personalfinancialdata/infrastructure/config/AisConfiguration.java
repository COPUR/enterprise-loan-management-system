package com.enterprise.openfinance.personalfinancialdata.infrastructure.config;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AisSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({AisCacheProperties.class, AisPaginationProperties.class})
public class AisConfiguration {

    @Bean
    public Clock accountInformationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AisSettings aisSettings(AisCacheProperties cacheProperties,
                                   AisPaginationProperties paginationProperties) {
        return new AisSettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
