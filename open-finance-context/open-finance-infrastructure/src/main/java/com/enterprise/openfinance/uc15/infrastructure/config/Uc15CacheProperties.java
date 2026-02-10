package com.enterprise.openfinance.uc15.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.uc15.cache")
public class Uc15CacheProperties {

    private Duration ttl = Duration.ofSeconds(60);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
