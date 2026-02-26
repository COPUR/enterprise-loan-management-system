package com.enterprise.openfinance.dynamiconboarding.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "open-finance.dynamic-onboarding.compliance")
public class DynamicOnboardingComplianceProperties {

    private List<String> blockedNames = new ArrayList<>(List.of("TEST_BLOCKED"));

    public List<String> getBlockedNames() {
        return blockedNames;
    }

    public void setBlockedNames(List<String> blockedNames) {
        this.blockedNames = blockedNames;
    }
}
