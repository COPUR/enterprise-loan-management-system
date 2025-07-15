package com.bank.loan.domain;

import com.bank.shared.kernel.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a unique payment identifier
 * 
 * This immutable identifier ensures each payment transaction
 * has a unique, traceable reference within the loan domain.
 * 
 * GRASP Principles:
 * - Information Expert: Knows how to generate and validate payment IDs
 * - Low Coupling: Simple value object with minimal dependencies
 */
public final class PaymentId implements ValueObject {
    
    private final String value;
    
    private PaymentId(String value) {
        this.value = Objects.requireNonNull(value, "Payment ID value cannot be null");
        validate(value);
    }
    
    /**
     * Generate a new unique payment ID
     */
    public static PaymentId generate() {
        return new PaymentId(UUID.randomUUID().toString());
    }
    
    /**
     * Create a payment ID from an existing value
     */
    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
    
    /**
     * Get the string value of the payment ID
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Validate the payment ID format
     */
    private void validate(String value) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be empty");
        }
        
        // Additional validation rules can be added here
        // For example, format validation, length checks, etc.
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PaymentId paymentId = (PaymentId) obj;
        return Objects.equals(value, paymentId.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return String.format("PaymentId{value='%s'}", value);
    }
}