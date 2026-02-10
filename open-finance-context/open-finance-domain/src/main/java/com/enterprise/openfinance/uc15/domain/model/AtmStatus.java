package com.enterprise.openfinance.uc15.domain.model;

public enum AtmStatus {
    IN_SERVICE("InService"),
    OUT_OF_SERVICE("OutOfService");

    private final String apiValue;

    AtmStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
