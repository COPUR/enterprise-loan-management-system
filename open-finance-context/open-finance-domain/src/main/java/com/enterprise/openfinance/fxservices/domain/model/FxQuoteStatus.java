package com.enterprise.openfinance.fxservices.domain.model;

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
