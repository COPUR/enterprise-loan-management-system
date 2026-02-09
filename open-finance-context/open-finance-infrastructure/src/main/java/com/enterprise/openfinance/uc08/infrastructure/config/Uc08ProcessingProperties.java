package com.enterprise.openfinance.uc08.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openfinance.uc08.processing")
public class Uc08ProcessingProperties {

    private long maxFileSizeBytes = 10_000_000L;
    private int statusPollsToComplete = 2;

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public int getStatusPollsToComplete() {
        return statusPollsToComplete;
    }

    public void setStatusPollsToComplete(int statusPollsToComplete) {
        this.statusPollsToComplete = statusPollsToComplete;
    }
}
