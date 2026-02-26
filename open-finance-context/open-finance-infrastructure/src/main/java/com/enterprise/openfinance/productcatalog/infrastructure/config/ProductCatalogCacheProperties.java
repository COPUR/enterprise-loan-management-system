package com.enterprise.openfinance.productcatalog.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.productcatalog.cache")
public class ProductCatalogCacheProperties {

    private Duration ttl = Duration.ofSeconds(60);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
