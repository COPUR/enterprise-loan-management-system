package com.enterprise.openfinance.corporatetreasury.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.corporatetreasury.cache")
public class CorporateTreasuryCacheProperties {

    private Duration ttl = Duration.ofSeconds(30);
    private int maxEntries = 10_000;

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }
}
