package com.enterprise.openfinance.uc05.infrastructure.config;

import com.enterprise.openfinance.uc05.domain.model.CorporateTreasurySettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc05CacheProperties.class, Uc05PaginationProperties.class})
public class Uc05Configuration {

    @Bean
    public Clock corporateTreasuryClock() {
        return Clock.systemUTC();
    }

    @Bean
    public CorporateTreasurySettings corporateTreasurySettings(Uc05CacheProperties cacheProperties,
                                                               Uc05PaginationProperties paginationProperties) {
        return new CorporateTreasurySettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
