package com.enterprise.openfinance.uc09.domain.model;

public enum MotorPolicyStatus {
    ACTIVE("Active", true),
    LAPSED("Lapsed", false),
    CANCELLED("Cancelled", false);

    private final String apiValue;
    private final boolean active;

    MotorPolicyStatus(String apiValue, boolean active) {
        this.apiValue = apiValue;
        this.active = active;
    }

    public String apiValue() {
        return apiValue;
    }

    public boolean isActive() {
        return active;
    }
}
