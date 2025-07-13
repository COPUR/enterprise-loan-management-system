package com.bank.loan.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Objects;

/**
 * Value object representing a unique customer identifier
 */
@Getter
@EqualsAndHashCode
@ToString
public class CustomerId {
    
    private final String value;
    
    public CustomerId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        this.value = value.trim();
    }
    
    public static CustomerId of(String value) {
        return new CustomerId(value);
    }
    
    public static CustomerId fromLong(Long id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return new CustomerId(id.toString());
    }
    
    public Long asLong() {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Customer ID is not a valid number: " + value);
        }
    }
    
    public boolean isEmpty() {
        return value.trim().isEmpty();
    }
}