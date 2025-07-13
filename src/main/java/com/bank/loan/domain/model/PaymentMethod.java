package com.bank.loan.domain.model;

/**
 * Enumeration representing different payment methods available in the system
 */
public enum PaymentMethod {
    BANK_TRANSFER("Bank Transfer", 0.0),
    CREDIT_CARD("Credit Card", 0.025),
    DEBIT_CARD("Debit Card", 0.01),
    CASH("Cash", 0.0),
    CHECK("Check", 0.005),
    ONLINE_BANKING("Online Banking", 0.0),
    MOBILE_PAYMENT("Mobile Payment", 0.015),
    CRYPTOCURRENCY("Cryptocurrency", 0.02),
    WIRE_TRANSFER("Wire Transfer", 0.01);
    
    private final String displayName;
    private final double processingFeeRate;
    
    PaymentMethod(String displayName, double processingFeeRate) {
        this.displayName = displayName;
        this.processingFeeRate = processingFeeRate;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public double getProcessingFeeRate() {
        return processingFeeRate;
    }
    
    public boolean requiresThirdPartyValidation() {
        return this == CREDIT_CARD || this == CRYPTOCURRENCY || this == WIRE_TRANSFER;
    }
    
    public boolean isInstantProcessing() {
        return this == CASH || this == DEBIT_CARD || this == ONLINE_BANKING || this == MOBILE_PAYMENT;
    }
}