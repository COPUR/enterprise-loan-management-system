package com.amanahfi.platform.events.port.in;

import com.amanahfi.platform.events.domain.EventStoreSnapshot;
import com.amanahfi.platform.events.domain.StoredEvent;

import java.util.List;
import java.util.Optional;

/**
 * Use case interface for event sourcing operations
 */
public interface EventSourcingUseCase {
    
    /**
     * Store events for an aggregate
     */
    void storeEvents(StoreEventsCommand command);
    
    /**
     * Get events for an aggregate from a specific version
     */
    List<StoredEvent> getEvents(GetEventsQuery query);
    
    /**
     * Get all events for an aggregate
     */
    List<StoredEvent> getAllEvents(GetAllEventsQuery query);
    
    /**
     * Get snapshot of event store
     */
    Optional<EventStoreSnapshot> getSnapshot(GetSnapshotQuery query);
    
    /**
     * Replay events from a specific version
     */
    void replayEvents(ReplayEventsCommand command);
    
    /**
     * Get events by category
     */
    List<StoredEvent> getEventsByCategory(GetEventsByCategoryQuery query);
    
    /**
     * Get events by time range
     */
    List<StoredEvent> getEventsByTimeRange(GetEventsByTimeRangeQuery query);
    
    /**
     * Check if aggregate exists
     */
    boolean aggregateExists(String aggregateId);
    
    /**
     * Get current version for aggregate
     */
    long getCurrentVersion(String aggregateId);
    
    /**
     * Delete event store (dangerous operation)
     */
    void deleteEventStore(DeleteEventStoreCommand command);
}