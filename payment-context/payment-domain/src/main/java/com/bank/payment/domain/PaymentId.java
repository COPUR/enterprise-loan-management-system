package com.bank.payment.domain;

import com.bank.shared.kernel.domain.ValueObject;

import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing a Payment ID
 */
public final class PaymentId implements ValueObject {
    
    private final String value;
    
    private PaymentId(String value) {
        this.value = Objects.requireNonNull(value, "Payment ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be empty");
        }
    }
    
    public static PaymentId of(String value) {
        return new PaymentId(value);
    }
    
    public static PaymentId generate() {
        return new PaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean isEmpty() {
        return value == null || value.trim().isEmpty();
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
        return "PaymentId{" + value + "}";
    }
}