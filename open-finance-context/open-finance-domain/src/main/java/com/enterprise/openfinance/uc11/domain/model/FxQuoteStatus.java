package com.enterprise.openfinance.uc11.domain.model;

public enum FxQuoteStatus {
    QUOTED("Quoted", false),
    BOOKED("Booked", true),
    EXPIRED("Expired", true);

    private final String apiValue;
    private final boolean terminal;

    FxQuoteStatus(String apiValue, boolean terminal) {
        this.apiValue = apiValue;
        this.terminal = terminal;
    }

    public String apiValue() {
        return apiValue;
    }

    public boolean isTerminal() {
        return terminal;
    }
}
