package com.enterprise.openfinance.payeeverification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openfinance.payeeverification.matching")
public class CoPMatchingProperties {

    private int closeMatchThreshold = 85;

    public int getCloseMatchThreshold() {
        return closeMatchThreshold;
    }

    public void setCloseMatchThreshold(int closeMatchThreshold) {
        this.closeMatchThreshold = closeMatchThreshold;
    }
}
