package com.bank.loanmanagement.customermanagement.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a unique customer identifier.
 * Immutable and type-safe to prevent primitive obsession.
 */
public final class CustomerId {
    
    private final String value;
    
    private CustomerId(String value) {
        this.value = Objects.requireNonNull(value, "Customer ID cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be empty");
        }
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
    
    public static CustomerId generate() {
        return new CustomerId("CUST-" + UUID.randomUUID().toString());
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerId that = (CustomerId) o;
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