package com.amanahfi.platform.events.domain;

import com.amanahfi.platform.shared.domain.DomainEvent;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Event Store for storing and retrieving domain events
 * Core component of event sourcing architecture
 */
@Getter
@ToString
@Builder
public class EventStore {
    
    private final EventStoreId eventStoreId;
    private final String aggregateId;
    private final String aggregateType;
    private final List<StoredEvent> events;
    private final long version;
    private final Instant createdAt;
    private final Instant lastUpdated;
    
    /**
     * Create a new event store for an aggregate
     */
    public static EventStore createForAggregate(String aggregateId, String aggregateType) {
        return EventStore.builder()
            .eventStoreId(EventStoreId.generate())
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .events(List.of())
            .version(0L)
            .createdAt(Instant.now())
            .lastUpdated(Instant.now())
            .build();
    }
    
    /**
     * Append events to the store
     */
    public EventStore appendEvents(List<DomainEvent> domainEvents, long expectedVersion) {
        validateVersion(expectedVersion);
        
        List<StoredEvent> storedEvents = domainEvents.stream()
            .map(event -> StoredEvent.fromDomainEvent(event, version + 1))
            .toList();
        
        return EventStore.builder()
            .eventStoreId(eventStoreId)
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .events(concatenateEvents(events, storedEvents))
            .version(version + storedEvents.size())
            .createdAt(createdAt)
            .lastUpdated(Instant.now())
            .build();
    }
    
    /**
     * Get events from a specific version
     */
    public List<StoredEvent> getEventsFromVersion(long fromVersion) {
        return events.stream()
            .filter(event -> event.getVersion() >= fromVersion)
            .toList();
    }
    
    /**
     * Get all events
     */
    public List<StoredEvent> getAllEvents() {
        return List.copyOf(events);
    }
    
    /**
     * Get the latest event
     */
    public Optional<StoredEvent> getLatestEvent() {
        return events.isEmpty() ? Optional.empty() : Optional.of(events.get(events.size() - 1));
    }
    
    /**
     * Check if the store has events
     */
    public boolean hasEvents() {
        return !events.isEmpty();
    }
    
    /**
     * Get events count
     */
    public int getEventCount() {
        return events.size();
    }
    
    /**
     * Get snapshot information
     */
    public EventStoreSnapshot getSnapshot() {
        return EventStoreSnapshot.builder()
            .eventStoreId(eventStoreId)
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .version(version)
            .eventCount(events.size())
            .createdAt(createdAt)
            .lastUpdated(lastUpdated)
            .build();
    }
    
    private void validateVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new OptimisticLockException(
                String.format("Expected version %d but current version is %d for aggregate %s", 
                    expectedVersion, version, aggregateId)
            );
        }
    }
    
    private List<StoredEvent> concatenateEvents(List<StoredEvent> existingEvents, List<StoredEvent> newEvents) {
        return java.util.stream.Stream.concat(existingEvents.stream(), newEvents.stream())
            .toList();
    }
}