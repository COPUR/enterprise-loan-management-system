package com.enterprise.openfinance.accountinformation.infrastructure.config;

import com.enterprise.openfinance.accountinformation.domain.model.AisSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({AccountInformationCacheProperties.class, AccountInformationPaginationProperties.class})
public class AccountInformationConfiguration {

    @Bean
    public Clock accountInformationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AisSettings aisSettings(AccountInformationCacheProperties cacheProperties,
                                   AccountInformationPaginationProperties paginationProperties) {
        return new AisSettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
