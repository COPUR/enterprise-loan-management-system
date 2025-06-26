package com.banking.loan.domain.shared;

import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base aggregate root for DDD implementation
 * Provides event sourcing capabilities and domain event publishing
 */
@Getter
public abstract class AggregateRoot<ID> {
    
    protected ID id;
    protected Long version;
    protected Instant createdAt;
    protected Instant updatedAt;
    protected String createdBy;
    protected String updatedBy;
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    protected AggregateRoot() {
        this.version = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
    
    protected AggregateRoot(ID id, String createdBy) {
        this();
        this.id = id;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
    }
    
    /**
     * Add domain event to be published
     */
    protected void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }
    
    /**
     * Get all domain events and clear the list
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
    
    /**
     * Get domain events without clearing (for read-only access)
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }
    
    /**
     * Clear all domain events
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
    
    /**
     * Increment version (for optimistic locking)
     */
    protected void incrementVersion() {
        this.version++;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Update the last modified information
     */
    protected void updateModifiedInfo(String updatedBy) {
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
        incrementVersion();
    }
    
    /**
     * Check if this aggregate has unsaved changes (domain events)
     */
    public boolean hasChanges() {
        return !domainEvents.isEmpty();
    }
    
    /**
     * Mark aggregate as committed (usually called after successful persistence)
     */
    public void markEventsAsCommitted() {
        domainEvents.clear();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) obj;
        return id != null && id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}