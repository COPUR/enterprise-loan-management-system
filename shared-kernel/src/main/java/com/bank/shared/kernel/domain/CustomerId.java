package com.bank.shared.kernel.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique customer identifier
 * 
 * This is in the shared kernel because customer identification
 * is used across multiple bounded contexts.
 */
public final class CustomerId implements ValueObject {
    
    private final String value;
    
    private CustomerId(String value) {
        this.value = Objects.requireNonNull(value, "Customer ID cannot be null");
    }
    
    public static CustomerId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        return new CustomerId(value.trim());
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    
    public static CustomerId fromLong(Long id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return new CustomerId("CUST-" + String.format("%08d", id));
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CustomerId that = (CustomerId) obj;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}