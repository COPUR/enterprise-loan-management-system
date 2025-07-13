package com.bank.shared.kernel.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for Aggregate Roots in Domain-Driven Design
 * 
 * An Aggregate Root is the only member of its aggregate that outside
 * objects are allowed to hold references to. It enforces the business
 * rules and invariants for the entire aggregate.
 * 
 * Supports Event-Driven Architecture (EDA) through domain event management
 * 
 * @param <ID> The type of the aggregate root identifier
 */
public abstract class AggregateRoot<ID> implements Entity<ID> {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    private Long version = 0L; // For optimistic locking
    
    /**
     * Add a domain event to be published
     * @param event the domain event to add
     */
    protected void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }
    
    /**
     * Get all domain events that have been recorded
     * @return immutable list of domain events
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    /**
     * Clear all recorded domain events
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
    
    /**
     * Mark this aggregate as having events that need to be published
     * @return true if there are unpublished events
     */
    public boolean hasUnpublishedEvents() {
        return !domainEvents.isEmpty();
    }
    
    /**
     * Get the version for optimistic locking
     */
    public Long getVersion() {
        return version;
    }
    
    /**
     * Set the version (used by infrastructure)
     */
    public void setVersion(Long version) {
        this.version = version;
    }
    
    /**
     * Mark aggregate as modified (increment version)
     */
    protected void markModified() {
        this.version++;
    }
}