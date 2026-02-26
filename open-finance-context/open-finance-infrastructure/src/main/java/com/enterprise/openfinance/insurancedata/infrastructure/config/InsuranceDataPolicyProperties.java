package com.enterprise.openfinance.insurancedata.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "open-finance.insurance-data.policy")
public class InsuranceDataPolicyProperties {

    private int defaultPageSize = 50;
    private int maxPageSize = 200;

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}
