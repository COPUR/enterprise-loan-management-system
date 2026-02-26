package com.enterprise.openfinance.fxservices.domain.model;

public enum FxDealStatus {
    BOOKED("Booked");

    private final String apiValue;

    FxDealStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
