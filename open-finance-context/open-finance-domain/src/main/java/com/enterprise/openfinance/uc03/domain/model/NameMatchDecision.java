package com.enterprise.openfinance.uc03.domain.model;

public enum NameMatchDecision {
    MATCH("Match"),
    CLOSE_MATCH("CloseMatch"),
    NO_MATCH("NoMatch"),
    UNABLE_TO_CHECK("UnableToCheck");

    private final String apiValue;

    NameMatchDecision(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
