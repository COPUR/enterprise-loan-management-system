package com.enterprise.openfinance.insurancequotes.domain.model;

public enum QuoteStatus {
    QUOTED("Quoted", false),
    ACCEPTED("Accepted", true),
    EXPIRED("Expired", true);

    private final String apiValue;
    private final boolean terminal;

    QuoteStatus(String apiValue, boolean terminal) {
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
