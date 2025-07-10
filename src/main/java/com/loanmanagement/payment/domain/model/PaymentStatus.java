package com.loanmanagement.payment.domain.model;

public enum PaymentStatus {
    PENDING("Payment due but not yet made"),
    COMPLETED("Payment successfully processed"),
    OVERDUE("Payment past due date"),
    CANCELLED("Payment cancelled"),
    FAILED("Payment processing failed");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }
    
    public boolean requiresAction() {
        return this == PENDING || this == OVERDUE || this == FAILED;
    }
}