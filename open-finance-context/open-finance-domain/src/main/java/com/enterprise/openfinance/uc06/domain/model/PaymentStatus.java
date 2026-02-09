package com.enterprise.openfinance.uc06.domain.model;

public enum PaymentStatus {
    PENDING("Pending"),
    ACCEPTED_SETTLEMENT_IN_PROCESS("AcceptedSettlementInProcess"),
    REJECTED("Rejected");

    private final String apiValue;

    PaymentStatus(String apiValue) {
        this.apiValue = apiValue;
    }

    public String apiValue() {
        return apiValue;
    }
}
