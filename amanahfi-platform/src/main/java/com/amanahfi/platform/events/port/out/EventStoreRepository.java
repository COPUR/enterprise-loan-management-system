package com.amanahfi.platform.events.port.out;

import com.amanahfi.platform.events.domain.EventStore;
import com.amanahfi.platform.events.domain.EventStoreId;

import java.util.Optional;

/**
 * Repository interface for event store persistence
 */
public interface EventStoreRepository {
    
    /**
     * Find event store by aggregate ID
     */
    Optional<EventStore> findByAggregateId(String aggregateId);
    
    /**
     * Find event store by ID
     */
    Optional<EventStore> findById(EventStoreId id);
    
    /**
     * Save event store
     */
    EventStore save(EventStore eventStore);
    
    /**
     * Delete event store
     */
    void delete(EventStore eventStore);
    
    /**
     * Check if event store exists for aggregate
     */
    boolean existsByAggregateId(String aggregateId);
    
    /**
     * Get event store count
     */
    long count();
    
    /**
     * Delete all event stores (for testing)
     */
    void deleteAll();
}