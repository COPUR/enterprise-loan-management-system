package com.enterprise.openfinance.uc14.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.uc14.cache")
public class Uc14CacheProperties {

    private Duration ttl = Duration.ofSeconds(60);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }
}
