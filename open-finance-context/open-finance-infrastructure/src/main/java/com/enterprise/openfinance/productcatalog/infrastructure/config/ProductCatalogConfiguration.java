package com.enterprise.openfinance.productcatalog.infrastructure.config;

import com.enterprise.openfinance.productcatalog.domain.model.ProductDataSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(ProductCatalogCacheProperties.class)
public class ProductCatalogConfiguration {

    @Bean
    public Clock productClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProductDataSettings productDataSettings(ProductCatalogCacheProperties properties) {
        return new ProductDataSettings(properties.getTtl());
    }
}
