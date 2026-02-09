package com.enterprise.openfinance.uc04.infrastructure.config;

import com.enterprise.openfinance.uc04.domain.model.MetadataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({Uc04CacheProperties.class, Uc04PaginationProperties.class})
public class Uc04Configuration {

    @Bean
    public Clock metadataClock() {
        return Clock.systemUTC();
    }

    @Bean
    public MetadataSettings metadataSettings(Uc04CacheProperties cacheProperties,
                                             Uc04PaginationProperties paginationProperties) {
        return new MetadataSettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
