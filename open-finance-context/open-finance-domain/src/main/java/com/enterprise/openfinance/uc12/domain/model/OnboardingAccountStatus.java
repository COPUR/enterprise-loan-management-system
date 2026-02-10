package com.enterprise.openfinance.uc12.domain.model;

public enum OnboardingAccountStatus {
    OPENED("Opened"),
    REJECTED("Rejected");

    private final String apiValue;

    OnboardingAccountStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
