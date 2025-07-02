package com.bank.payment.domain.model;

import com.bank.loan.domain.shared.DomainId;

/**
 * BIAN-compliant Payment Initiation Instance Reference
 * Uniquely identifies a payment initiation arrangement instance
 */
public class PaymentInitiationInstanceReference extends DomainId {
    
    private static final String PREFIX = "PII-";
    
    public PaymentInitiationInstanceReference(String value) {
        super(value);
        validateFormat(value);
    }
    
    public PaymentInitiationInstanceReference() {
        super();
    }
    
    @Override
    protected String generateNewId() {
        return PREFIX + super.generateNewId();
    }
    
    private void validateFormat(String value) {
        if (!value.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Payment Initiation Instance Reference must start with " + PREFIX);
        }
        if (value.length() < 10) {
            throw new IllegalArgumentException("Payment Initiation Instance Reference must be at least 10 characters long");
        }
    }
    
    public static PaymentInitiationInstanceReference of(String value) {
        return new PaymentInitiationInstanceReference(value);
    }
    
    public static PaymentInitiationInstanceReference generate() {
        return new PaymentInitiationInstanceReference();
    }
}