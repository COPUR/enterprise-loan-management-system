package com.loanmanagement.payment.domain.model;

/**
 * Enum representing different types of payment modifications.
 */
public enum PaymentModificationType {
    
    AMOUNT_CHANGE("Payment amount was changed"),
    DUE_DATE_CHANGE("Payment due date was changed"),
    METHOD_CHANGE("Payment method was changed"),
    FREQUENCY_CHANGE("Payment frequency was changed"),
    STATUS_CHANGE("Payment status was changed"),
    CURRENCY_CHANGE("Payment currency was changed"),
    SCHEDULE_CHANGE("Payment schedule was changed"),
    ALLOCATION_CHANGE("Payment allocation was changed"),
    PENALTY_ADJUSTMENT("Payment penalty was adjusted"),
    DISCOUNT_ADJUSTMENT("Payment discount was adjusted");
    
    private final String description;
    
    PaymentModificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isAmountRelated() {
        return this == AMOUNT_CHANGE || this == PENALTY_ADJUSTMENT || this == DISCOUNT_ADJUSTMENT;
    }
    
    public boolean isScheduleRelated() {
        return this == DUE_DATE_CHANGE || this == FREQUENCY_CHANGE || this == SCHEDULE_CHANGE;
    }
    
    public boolean isMethodRelated() {
        return this == METHOD_CHANGE || this == CURRENCY_CHANGE;
    }
}