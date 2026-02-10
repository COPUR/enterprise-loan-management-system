package com.enterprise.openfinance.businessfinancialdata.infrastructure.config;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTreasurySettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({CorporateCacheProperties.class, CorporatePaginationProperties.class})
public class CorporateConfiguration {

    @Bean
    public Clock corporateTreasuryClock() {
        return Clock.systemUTC();
    }

    @Bean
    public CorporateTreasurySettings corporateTreasurySettings(CorporateCacheProperties cacheProperties,
                                                               CorporatePaginationProperties paginationProperties) {
        return new CorporateTreasurySettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
