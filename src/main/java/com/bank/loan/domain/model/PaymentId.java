package com.bank.loan.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique payment identifier
 */
@Getter
@EqualsAndHashCode
@ToString
public class PaymentId {
    
    private final String value;
    
    private PaymentId(String value) {
        this.value = Objects.requireNonNull(value, "Payment ID cannot be null");
    }
    
    public static PaymentId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID cannot be null or empty");
        }
        return new PaymentId(value.trim());
    }
    
    public static PaymentId generate() {
        return new PaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    
    public static PaymentId fromLong(Long id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return new PaymentId("PAY-" + String.format("%08d", id));
    }
    
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
}