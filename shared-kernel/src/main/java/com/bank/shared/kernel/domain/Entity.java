package com.bank.shared.kernel.domain;

/**
 * Base interface for Domain Entities in Domain-Driven Design
 * 
 * Entities have conceptual identity that runs through time and often
 * through distinct representations. They are defined by their identity
 * rather than their attributes.
 * 
 * @param <ID> The type of the entity identifier
 */
public interface Entity<ID> {
    
    /**
     * Get the unique identifier for this entity
     * @return the entity identifier
     */
    ID getId();
    
    /**
     * Check if this entity has the same identity as another
     * @param other the other entity to compare
     * @return true if they have the same identity
     */
    default boolean sameIdentityAs(Entity<ID> other) {
        return other != null && getId() != null && getId().equals(other.getId());
    }
}