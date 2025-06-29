package com.bank.loanmanagement.domain.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base class for all aggregate roots in the loan management system.
 * 
 * Implements DDD Aggregate Root pattern with Event-Driven Communication.
 * Provides domain event management for proper event sourcing and publishing.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Clear abstraction and single responsibility
 * ✅ DDD: Proper aggregate root implementation
 * ✅ Event-Driven: Domain event collection and publishing
 * ✅ Hexagonal: Pure domain concept, no infrastructure dependencies
 */
public abstract class AggregateRoot<ID> {
    
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    
    /**
     * Add a domain event to be published later
     */
    protected void addDomainEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Domain event cannot be null");
        domainEvents.add(event);
    }
    
    /**
     * Get all uncommitted domain events
     */
    public List<DomainEvent> getUncommittedEvents() {
        return List.copyOf(domainEvents);
    }
    
    /**
     * Mark all domain events as committed (published)
     */
    public void markEventsAsCommitted() {
        domainEvents.clear();
    }
    
    /**
     * @deprecated Use getUncommittedEvents() instead
     */
    @Deprecated(since = "2024-06", forRemoval = true)
    public List<DomainEvent> getDomainEvents() {
        return getUncommittedEvents();
    }
    
    /**
     * @deprecated Use markEventsAsCommitted() instead
     */
    @Deprecated(since = "2024-06", forRemoval = true)
    public void clearDomainEvents() {
        markEventsAsCommitted();
    }
    
    /**
     * Check if there are uncommitted events
     */
    public boolean hasUncommittedEvents() {
        return !domainEvents.isEmpty();
    }
    
    /**
     * Get the number of uncommitted events
     */
    public int getUncommittedEventCount() {
        return domainEvents.size();
    }
    
    /**
     * Business method to get the most recent event
     */
    public DomainEvent getLastEvent() {
        if (domainEvents.isEmpty()) {
            return null;
        }
        return domainEvents.get(domainEvents.size() - 1);
    }
    
    /**
     * Business method to check if a specific event type exists
     */
    public boolean hasEventOfType(Class<? extends DomainEvent> eventType) {
        return domainEvents.stream()
                .anyMatch(event -> eventType.isInstance(event));
    }
    
    /**
     * Business method to get events of a specific type
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> getEventsOfType(Class<T> eventType) {
        return domainEvents.stream()
                .filter(eventType::isInstance)
                .map(event -> (T) event)
                .toList();
    }
    
    /**
     * Abstract method to get the aggregate's unique identifier
     */
    public abstract ID getId();
    
    /**
     * Business method to get aggregate identifier as string
     */
    public String getAggregateId() {
        ID id = getId();
        return id != null ? id.toString() : null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AggregateRoot)) return false;
        AggregateRoot<?> that = (AggregateRoot<?>) o;
        return Objects.equals(getId(), that.getId());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
    
    @Override
    public String toString() {
        return String.format("%s{id=%s, uncommittedEvents=%d}", 
                           getClass().getSimpleName(), getId(), getUncommittedEventCount());
    }
}