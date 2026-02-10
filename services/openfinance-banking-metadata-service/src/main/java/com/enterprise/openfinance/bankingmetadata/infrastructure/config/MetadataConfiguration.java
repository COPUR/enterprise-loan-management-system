package com.enterprise.openfinance.bankingmetadata.infrastructure.config;

import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({MetadataCacheProperties.class, MetadataPaginationProperties.class})
public class MetadataConfiguration {

    @Bean
    public Clock metadataClock() {
        return Clock.systemUTC();
    }

    @Bean
    public MetadataSettings metadataSettings(MetadataCacheProperties cacheProperties,
                                             MetadataPaginationProperties paginationProperties) {
        return new MetadataSettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
