package com.enterprise.openfinance.corporatetreasury.infrastructure.config;

import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateTreasurySettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({CorporateTreasuryCacheProperties.class, CorporateTreasuryPaginationProperties.class})
public class CorporateTreasuryConfiguration {

    @Bean
    public Clock corporateTreasuryClock() {
        return Clock.systemUTC();
    }

    @Bean
    public CorporateTreasurySettings corporateTreasurySettings(CorporateTreasuryCacheProperties cacheProperties,
                                                               CorporateTreasuryPaginationProperties paginationProperties) {
        return new CorporateTreasurySettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
