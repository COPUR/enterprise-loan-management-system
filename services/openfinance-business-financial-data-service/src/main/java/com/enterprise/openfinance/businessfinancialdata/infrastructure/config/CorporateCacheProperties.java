package com.enterprise.openfinance.businessfinancialdata.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openfinance.businessfinancialdata.cache")
public class CorporateCacheProperties {

    private Duration ttl = Duration.ofSeconds(30);
    private Duration etagTtl = Duration.ofSeconds(30);
    private int maxEntries = 10_000;
    private String keyPrefix = "openfinance:businessfinancialdata";

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

    public Duration getEtagTtl() {
        return etagTtl;
    }

    public void setEtagTtl(Duration etagTtl) {
        this.etagTtl = etagTtl;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }
}
