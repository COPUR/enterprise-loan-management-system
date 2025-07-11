package com.amanahfi.platform.shared.domain;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for all aggregate roots in the AmanahFi Platform
 * 
 * This class provides the foundation for domain-driven design aggregate roots
 * with built-in event handling capabilities. All business entities that serve
 * as aggregate roots should extend this class.
 * 
 * Key Features:
 * - Domain event collection and management
 * - Defensive programming with null safety
 * - Clean event handling without external frameworks
 * - Sharia compliance tracking capability
 * - Audit trail support
 * 
 * Design Principles:
 * - Single Responsibility: Manages domain events
 * - Open/Closed: Extensible for different aggregate types
 * - Liskov Substitution: All aggregates can be treated uniformly
 * - Interface Segregation: Minimal required interface
 * - Dependency Inversion: Depends on abstractions
 * 
 * @param <ID> The type of the aggregate's identifier
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@Slf4j
@Getter
public abstract class AggregateRoot<ID> {

    /**
     * The unique identifier for this aggregate
     */
    protected ID id;

    /**
     * Collection of uncommitted domain events
     * Using ArrayList for performance and mutability
     */
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    /**
     * Version for optimistic locking and concurrency control
     */
    protected Long version = 0L;

    /**
     * Protected constructor to ensure proper initialization
     * 
     * @param id The unique identifier for this aggregate
     * @throws IllegalArgumentException if id is null
     */
    protected AggregateRoot(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("Aggregate ID cannot be null");
        }
        this.id = id;
        log.debug("Created aggregate root with ID: {}", id);
    }

    /**
     * Default constructor for frameworks that require it
     * WARNING: Should only be used by JPA/ORM frameworks
     */
    protected AggregateRoot() {
        // Framework constructor - ID will be set by persistence layer
    }

    /**
     * Applies a domain event to this aggregate
     * 
     * This method adds the event to the uncommitted events collection
     * and logs the event for audit purposes. The event will be published
     * when the aggregate is saved.
     * 
     * @param event The domain event to apply
     * @throws IllegalArgumentException if event is null
     */
    protected void applyEvent(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Domain event cannot be null");
        }

        // Defensive programming: validate event before adding
        if (event.getAggregateId() == null) {
            throw new IllegalArgumentException("Domain event must have an aggregate ID");
        }

        if (event.getOccurredOn() == null) {
            throw new IllegalArgumentException("Domain event must have an occurrence timestamp");
        }

        uncommittedEvents.add(event);
        this.version++;
        
        log.debug("Applied domain event: {} to aggregate: {}", 
            event.getClass().getSimpleName(), this.id);
    }

    /**
     * Gets all uncommitted events for this aggregate
     * 
     * Returns an immutable view of the uncommitted events to prevent
     * external modification. This supports the encapsulation principle.
     * 
     * @return Immutable list of uncommitted domain events
     */
    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    /**
     * Marks all events as committed
     * 
     * This method should be called after the events have been successfully
     * published to the event store. It clears the uncommitted events collection.
     */
    public void markEventsAsCommitted() {
        int eventCount = uncommittedEvents.size();
        uncommittedEvents.clear();
        
        log.debug("Marked {} events as committed for aggregate: {}", eventCount, this.id);
    }

    /**
     * Checks if this aggregate has uncommitted events
     * 
     * @return true if there are uncommitted events, false otherwise
     */
    public boolean hasUncommittedEvents() {
        return !uncommittedEvents.isEmpty();
    }

    /**
     * Gets the current version of this aggregate
     * 
     * The version is incremented each time an event is applied,
     * supporting optimistic locking and concurrency control.
     * 
     * @return The current version number
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version (used by persistence layer)
     * 
     * @param version The version to set
     */
    protected void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Gets the aggregate identifier
     * 
     * @return The unique identifier for this aggregate
     */
    public ID getId() {
        return id;
    }

    /**
     * Equality based on aggregate ID
     * 
     * Two aggregates are considered equal if they have the same ID
     * and are of the same type.
     * 
     * @param other The object to compare with
     * @return true if aggregates are equal, false otherwise
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        
        AggregateRoot<?> that = (AggregateRoot<?>) other;
        return id != null && id.equals(that.id);
    }

    /**
     * Hash code based on aggregate ID
     * 
     * @return Hash code of the aggregate ID
     */
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * String representation of the aggregate
     * 
     * @return String representation including class name and ID
     */
    @Override
    public String toString() {
        return String.format("%s{id=%s, version=%d}", 
            getClass().getSimpleName(), id, version);
    }
}