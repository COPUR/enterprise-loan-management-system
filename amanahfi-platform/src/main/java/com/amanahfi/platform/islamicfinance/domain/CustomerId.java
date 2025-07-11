package com.amanahfi.platform.islamicfinance.domain;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing the unique identifier for Customers
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
public class CustomerId {
    
    UUID value;

    public CustomerId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("CustomerId cannot be null");
        }
        this.value = value;
    }

    public static CustomerId generate() {
        return new CustomerId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}