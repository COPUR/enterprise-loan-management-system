package com.amanahfi.platform.islamicfinance.domain;

import lombok.Value;

import java.util.UUID;

/**
 * Value object representing the unique identifier for Islamic Finance Products
 * 
 * This strongly-typed identifier ensures type safety and prevents mixing
 * of different entity identifiers throughout the system.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Value
public class IslamicFinanceProductId {
    
    UUID value;

    public IslamicFinanceProductId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("IslamicFinanceProductId cannot be null");
        }
        this.value = value;
    }

    public static IslamicFinanceProductId generate() {
        return new IslamicFinanceProductId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}