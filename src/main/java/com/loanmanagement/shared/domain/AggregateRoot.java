package com.loanmanagement.shared.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for Aggregate Roots in Domain-Driven Design
 * Handles domain events and ensures aggregate consistency
 */
public abstract class AggregateRoot<ID extends DomainId> {
    
    private final ID id;
    private final List<Object> uncommittedEvents = new ArrayList<>();

    protected AggregateRoot(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    /**
     * Register a domain event to be published
     */
    protected void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    /**
     * Get all uncommitted events
     */
    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    /**
     * Mark all events as committed (usually called after publishing)
     */
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    /**
     * Check if there are uncommitted events
     */
    public boolean hasUncommittedEvents() {
        return !uncommittedEvents.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        AggregateRoot<?> that = (AggregateRoot<?>) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}