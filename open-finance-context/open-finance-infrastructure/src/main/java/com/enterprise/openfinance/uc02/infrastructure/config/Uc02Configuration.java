package com.enterprise.openfinance.uc02.infrastructure.config;

import com.enterprise.openfinance.uc02.domain.model.AisSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc02CacheProperties.class, Uc02PaginationProperties.class})
public class Uc02Configuration {

    @Bean
    public Clock accountInformationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public AisSettings aisSettings(Uc02CacheProperties cacheProperties,
                                   Uc02PaginationProperties paginationProperties) {
        return new AisSettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
