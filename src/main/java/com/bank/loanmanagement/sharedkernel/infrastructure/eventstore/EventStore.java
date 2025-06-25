package com.bank.loanmanagement.sharedkernel.infrastructure.eventstore;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;

import java.util.List;
import java.util.Optional;

/**
 * Event Store interface for Event Sourcing implementation
 * Provides foundation for Event-Driven Architecture
 * Following BIAN event persistence patterns
 */
public interface EventStore {

    /**
     * Save domain events to the event store
     * Ensures atomic persistence with proper ordering
     */
    void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion);

    /**
     * Retrieve all events for a specific aggregate
     * Ordered by version for proper event replay
     */
    List<DomainEvent> getEventsForAggregate(String aggregateId);

    /**
     * Retrieve events for aggregate starting from specific version
     * Used for incremental event replay and projections
     */
    List<DomainEvent> getEventsForAggregateFromVersion(String aggregateId, long fromVersion);

    /**
     * Retrieve events by event type
     * Used for creating read models and projections
     */
    List<DomainEvent> getEventsByType(String eventType);

    /**
     * Retrieve events by BIAN service domain
     * Enables service domain specific event processing
     */
    List<DomainEvent> getEventsByServiceDomain(String serviceDomain);

    /**
     * Retrieve latest aggregate version
     * Used for optimistic concurrency control
     */
    Optional<Long> getLatestAggregateVersion(String aggregateId);

    /**
     * Retrieve all events after specific timestamp
     * Used for event streaming and catch-up subscriptions
     */
    List<DomainEvent> getEventsAfter(java.time.OffsetDateTime timestamp);

    /**
     * Create snapshot of aggregate state
     * Optimization for large event streams
     */
    void saveSnapshot(String aggregateId, Object snapshot, long version);

    /**
     * Retrieve latest snapshot for aggregate
     * Used to optimize event replay performance
     */
    Optional<AggregateSnapshot> getLatestSnapshot(String aggregateId);

    /**
     * Health check for event store
     */
    boolean isHealthy();

    /**
     * Event store statistics
     */
    EventStoreStatistics getStatistics();

    /**
     * Aggregate snapshot representation
     */
    record AggregateSnapshot(
        String aggregateId,
        String aggregateType,
        Object data,
        long version,
        java.time.OffsetDateTime createdAt
    ) {}

    /**
     * Event store operational statistics
     */
    record EventStoreStatistics(
        long totalEvents,
        long totalAggregates,
        long totalSnapshots,
        double averageEventsPerAggregate,
        java.time.OffsetDateTime lastEventTime
    ) {}
}