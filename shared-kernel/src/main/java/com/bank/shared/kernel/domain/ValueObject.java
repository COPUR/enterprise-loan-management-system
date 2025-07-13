package com.bank.shared.kernel.domain;

/**
 * Marker interface for Value Objects in Domain-Driven Design
 * 
 * Value Objects are immutable objects that represent descriptive aspects
 * of the domain with no conceptual identity. They are defined by their
 * attributes rather than their identity.
 * 
 * Characteristics:
 * - Immutable
 * - No identity
 * - Defined by their attributes
 * - Can be shared and cached
 * - Implement equals/hashCode based on attributes
 */
public interface ValueObject {
    
    /**
     * Check if this value object is empty or represents no value
     * @return true if empty, false otherwise
     */
    default boolean isEmpty() {
        return false;
    }
}