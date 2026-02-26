package com.enterprise.openfinance.bankingmetadata.infrastructure.config;

import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({BankingMetadataCacheProperties.class, BankingMetadataPaginationProperties.class})
public class BankingMetadataConfiguration {

    @Bean
    public Clock metadataClock() {
        return Clock.systemUTC();
    }

    @Bean
    public MetadataSettings metadataSettings(BankingMetadataCacheProperties cacheProperties,
                                             BankingMetadataPaginationProperties paginationProperties) {
        return new MetadataSettings(
                cacheProperties.getTtl(),
                paginationProperties.getDefaultPageSize(),
                paginationProperties.getMaxPageSize()
        );
    }
}
