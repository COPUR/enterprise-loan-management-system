package com.enterprise.openfinance.uc07.domain.model;

public enum VrpConsentStatus {
    AUTHORISED("Authorised", true),
    REVOKED("Revoked", false),
    EXPIRED("Expired", false);

    private final String apiValue;
    private final boolean usable;

    VrpConsentStatus(String apiValue, boolean usable) {
        this.apiValue = apiValue;
        this.usable = usable;
    }

    public String apiValue() {
        return apiValue;
    }

    public boolean isUsable() {
        return usable;
    }
}
