package com.enterprise.shared.domain;

import lombok.Value;

import java.util.UUID;

/**
 * Customer identifier value object.
 * Shared across all bounded contexts.
 */
@Value
public class CustomerId {
    String value;
    
    public static CustomerId generate() {
        return of(UUID.randomUUID().toString());
    }
    
    public static CustomerId of(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        return new CustomerId(value);
    }
}