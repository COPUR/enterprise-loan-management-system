package com.enterprise.openfinance.application.saga.model;

import lombok.Value;

import java.util.Objects;
import java.util.UUID;

/**
 * Strong-typed identifier for saga executions.
 * Provides uniqueness and type safety for saga identification.
 */
@Value(staticConstructor = "of")
public class SagaId {
    
    String value;
    
    /**
     * Generate a new unique saga ID.
     */
    public static SagaId generate() {
        return new SagaId("SAGA-" + UUID.randomUUID().toString().toUpperCase());
    }
    
    /**
     * Create from string value.
     */
    public static SagaId of(String value) {
        Objects.requireNonNull(value, "Saga ID value cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Saga ID value cannot be empty");
        }
        return new SagaId(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}