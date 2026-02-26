package com.enterprise.openfinance.dynamiconboarding.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "open-finance.dynamic-onboarding.cache")
public class DynamicOnboardingCacheProperties {

    private Duration idempotencyTtl = Duration.ofHours(24);
    private Duration cacheTtl = Duration.ofSeconds(30);
    private int maxEntries = 10_000;
    private String accountPrefix = "ACC";

    public Duration getIdempotencyTtl() {
        return idempotencyTtl;
    }

    public void setIdempotencyTtl(Duration idempotencyTtl) {
        this.idempotencyTtl = idempotencyTtl;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public String getAccountPrefix() {
        return accountPrefix;
    }

    public void setAccountPrefix(String accountPrefix) {
        this.accountPrefix = accountPrefix;
    }
}
