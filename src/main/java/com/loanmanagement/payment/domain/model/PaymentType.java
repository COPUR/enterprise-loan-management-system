package com.loanmanagement.payment.domain.model;

public enum PaymentType {
    REGULAR("Regular monthly payment"),
    PARTIAL("Partial payment"),
    FULL("Full loan payoff"),
    PENALTY("Penalty payment"),
    PREPAYMENT("Early payment");
    
    private final String description;
    
    PaymentType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isEarlyPayment() {
        return this == PREPAYMENT || this == FULL;
    }
}