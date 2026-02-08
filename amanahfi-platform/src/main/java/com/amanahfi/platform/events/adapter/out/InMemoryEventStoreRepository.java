package com.amanahfi.platform.events.adapter.out;

import com.amanahfi.platform.events.domain.EventStore;
import com.amanahfi.platform.events.domain.EventStoreId;
import com.amanahfi.platform.events.port.out.EventStoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of EventStoreRepository for testing and development
 */
@Repository
@Slf4j
public class InMemoryEventStoreRepository implements EventStoreRepository {
    
    private final ConcurrentHashMap<EventStoreId, EventStore> eventStores = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EventStoreId> aggregateIndex = new ConcurrentHashMap<>();
    
    @Override
    public Optional<EventStore> findByAggregateId(String aggregateId) {
        log.debug("Finding event store for aggregate: {}", aggregateId);
        
        EventStoreId id = aggregateIndex.get(aggregateId);
        if (id == null) {
            log.debug("No event store found for aggregate: {}", aggregateId);
            return Optional.empty();
        }
        
        EventStore eventStore = eventStores.get(id);
        log.debug("Found event store for aggregate: {} with {} events", aggregateId, 
            eventStore != null ? eventStore.getStoredEvents().size() : 0);
        
        return Optional.ofNullable(eventStore);
    }
    
    @Override
    public Optional<EventStore> findById(EventStoreId id) {
        log.debug("Finding event store by ID: {}", id);
        
        EventStore eventStore = eventStores.get(id);
        if (eventStore != null) {
            log.debug("Found event store: {} for aggregate: {}", id, eventStore.getAggregateId());
        } else {
            log.debug("No event store found for ID: {}", id);
        }
        
        return Optional.ofNullable(eventStore);
    }
    
    @Override
    public EventStore save(EventStore eventStore) {
        log.debug("Saving event store for aggregate: {} with {} events", 
            eventStore.getAggregateId(), eventStore.getStoredEvents().size());
        
        // Store in main map
        eventStores.put(eventStore.getId(), eventStore);
        
        // Update aggregate index
        aggregateIndex.put(eventStore.getAggregateId(), eventStore.getId());
        
        log.debug("Saved event store for aggregate: {} - Version: {}", 
            eventStore.getAggregateId(), eventStore.getVersion());
        
        return eventStore;
    }
    
    @Override
    public void delete(EventStore eventStore) {
        log.warn("Deleting event store for aggregate: {}", eventStore.getAggregateId());
        
        // Remove from main map
        eventStores.remove(eventStore.getId());
        
        // Remove from aggregate index
        aggregateIndex.remove(eventStore.getAggregateId());
        
        log.warn("Deleted event store for aggregate: {}", eventStore.getAggregateId());
    }
    
    @Override
    public boolean existsByAggregateId(String aggregateId) {
        boolean exists = aggregateIndex.containsKey(aggregateId);
        log.debug("Event store exists for aggregate: {} = {}", aggregateId, exists);
        return exists;
    }
    
    @Override
    public long count() {
        long count = eventStores.size();
        log.debug("Total event stores: {}", count);
        return count;
    }
    
    @Override
    public void deleteAll() {
        log.warn("Deleting all event stores - Count: {}", eventStores.size());
        eventStores.clear();
        aggregateIndex.clear();
        log.warn("Deleted all event stores");
    }
}