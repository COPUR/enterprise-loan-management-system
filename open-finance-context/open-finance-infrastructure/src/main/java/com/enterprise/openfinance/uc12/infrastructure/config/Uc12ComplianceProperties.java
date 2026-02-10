package com.enterprise.openfinance.uc12.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "open-finance.uc12.compliance")
public class Uc12ComplianceProperties {

    private List<String> blockedNames = new ArrayList<>(List.of("TEST_BLOCKED"));

    public List<String> getBlockedNames() {
        return blockedNames;
    }

    public void setBlockedNames(List<String> blockedNames) {
        this.blockedNames = blockedNames;
    }
}
