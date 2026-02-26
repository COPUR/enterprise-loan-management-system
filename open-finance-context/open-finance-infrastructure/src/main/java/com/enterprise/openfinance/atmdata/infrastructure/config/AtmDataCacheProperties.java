package com.enterprise.openfinance.atmdata.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.atmdata.cache")
public class AtmDataCacheProperties {

    private Duration ttl = Duration.ofSeconds(60);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
