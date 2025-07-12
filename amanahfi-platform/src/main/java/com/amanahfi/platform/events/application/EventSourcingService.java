package com.amanahfi.platform.events.application;

import com.amanahfi.platform.events.domain.*;
import com.amanahfi.platform.events.port.in.*;
import com.amanahfi.platform.events.port.out.EventStoreRepository;
import com.amanahfi.platform.events.port.out.EventStreamPublisher;
import com.amanahfi.platform.shared.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Event sourcing application service
 * Handles event storage, retrieval, and streaming
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class EventSourcingService implements EventSourcingUseCase {
    
    private final EventStoreRepository eventStoreRepository;
    private final EventStreamPublisher eventStreamPublisher;
    
    @Override
    public void storeEvents(StoreEventsCommand command) {
        log.info("Storing {} events for aggregate: {}", 
            command.getEvents().size(), command.getAggregateId());
        
        // Validate command
        command.validate();
        
        try {
            // Get or create event store
            EventStore eventStore = eventStoreRepository.findByAggregateId(command.getAggregateId())
                .orElseGet(() -> EventStore.createForAggregate(
                    command.getAggregateId(), command.getAggregateType()));
            
            // Append events with optimistic locking
            EventStore updatedStore = eventStore.appendEvents(
                command.getEvents(), command.getExpectedVersion());
            
            // Save to repository
            eventStoreRepository.save(updatedStore);
            
            // Publish events to stream
            publishEventsToStream(command.getEvents(), command.getAggregateId());
            
            log.info("Stored {} events for aggregate: {}", 
                command.getEvents().size(), command.getAggregateId());
            
        } catch (OptimisticLockException e) {
            log.error("Optimistic lock exception for aggregate: {}", command.getAggregateId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to store events for aggregate: {}", command.getAggregateId(), e);
            throw new EventStoreException("Failed to store events", e);
        }
    }
    
    @Override
    public List<StoredEvent> getEvents(GetEventsQuery query) {
        log.debug("Getting events for aggregate: {} from version: {}", 
            query.getAggregateId(), query.getFromVersion());
        
        // Validate query
        query.validate();
        
        Optional<EventStore> eventStore = eventStoreRepository.findByAggregateId(query.getAggregateId());
        
        if (eventStore.isEmpty()) {
            log.debug("No event store found for aggregate: {}", query.getAggregateId());
            return List.of();
        }
        
        List<StoredEvent> events = eventStore.get().getEventsFromVersion(query.getFromVersion());
        
        log.debug("Retrieved {} events for aggregate: {}", events.size(), query.getAggregateId());
        return events;
    }
    
    @Override
    public List<StoredEvent> getAllEvents(GetAllEventsQuery query) {
        log.debug("Getting all events for aggregate: {}", query.getAggregateId());
        
        // Validate query
        query.validate();
        
        Optional<EventStore> eventStore = eventStoreRepository.findByAggregateId(query.getAggregateId());
        
        if (eventStore.isEmpty()) {
            log.debug("No event store found for aggregate: {}", query.getAggregateId());
            return List.of();
        }
        
        List<StoredEvent> events = eventStore.get().getAllEvents();
        
        log.debug("Retrieved {} total events for aggregate: {}", events.size(), query.getAggregateId());
        return events;
    }
    
    @Override
    public Optional<EventStoreSnapshot> getSnapshot(GetSnapshotQuery query) {
        log.debug("Getting snapshot for aggregate: {}", query.getAggregateId());
        
        // Validate query
        query.validate();
        
        Optional<EventStore> eventStore = eventStoreRepository.findByAggregateId(query.getAggregateId());
        
        if (eventStore.isEmpty()) {
            log.debug("No event store found for aggregate: {}", query.getAggregateId());
            return Optional.empty();
        }
        
        EventStoreSnapshot snapshot = eventStore.get().getSnapshot();
        
        log.debug("Retrieved snapshot for aggregate: {} with version: {}", 
            query.getAggregateId(), snapshot.getVersion());
        return Optional.of(snapshot);
    }
    
    @Override
    public void replayEvents(ReplayEventsCommand command) {
        log.info("Replaying events for aggregate: {} from version: {}", 
            command.getAggregateId(), command.getFromVersion());
        
        // Validate command
        command.validate();
        
        try {
            // Get events from the specified version
            List<StoredEvent> events = getEvents(GetEventsQuery.builder()
                .aggregateId(command.getAggregateId())
                .fromVersion(command.getFromVersion())
                .build());
            
            if (events.isEmpty()) {
                log.info("No events to replay for aggregate: {}", command.getAggregateId());
                return;
            }
            
            // Convert stored events to domain events
            List<DomainEvent> domainEvents = events.stream()
                .map(StoredEvent::toDomainEvent)
                .toList();
            
            // Publish events to stream for replay
            publishEventsToStream(domainEvents, command.getAggregateId());
            
            log.info("Replayed {} events for aggregate: {}", events.size(), command.getAggregateId());
            
        } catch (Exception e) {
            log.error("Failed to replay events for aggregate: {}", command.getAggregateId(), e);
            throw new EventReplayException("Failed to replay events", e);
        }
    }
    
    @Override
    public List<StoredEvent> getEventsByCategory(GetEventsByCategoryQuery query) {
        log.debug("Getting events by category: {} for aggregate: {}", 
            query.getEventCategory(), query.getAggregateId());
        
        // Validate query
        query.validate();
        
        List<StoredEvent> allEvents = getAllEvents(GetAllEventsQuery.builder()
            .aggregateId(query.getAggregateId())
            .build());
        
        List<StoredEvent> filteredEvents = allEvents.stream()
            .filter(event -> event.getEventCategory() == query.getEventCategory())
            .toList();
        
        log.debug("Retrieved {} events of category: {} for aggregate: {}", 
            filteredEvents.size(), query.getEventCategory(), query.getAggregateId());
        return filteredEvents;
    }
    
    @Override
    public List<StoredEvent> getEventsByTimeRange(GetEventsByTimeRangeQuery query) {
        log.debug("Getting events by time range for aggregate: {} from: {} to: {}", 
            query.getAggregateId(), query.getFromTime(), query.getToTime());
        
        // Validate query
        query.validate();
        
        List<StoredEvent> allEvents = getAllEvents(GetAllEventsQuery.builder()
            .aggregateId(query.getAggregateId())
            .build());
        
        List<StoredEvent> filteredEvents = allEvents.stream()
            .filter(event -> !event.getTimestamp().isBefore(query.getFromTime()) &&
                           !event.getTimestamp().isAfter(query.getToTime()))
            .toList();
        
        log.debug("Retrieved {} events in time range for aggregate: {}", 
            filteredEvents.size(), query.getAggregateId());
        return filteredEvents;
    }
    
    @Override
    public boolean aggregateExists(String aggregateId) {
        return eventStoreRepository.findByAggregateId(aggregateId).isPresent();
    }
    
    @Override
    public long getCurrentVersion(String aggregateId) {
        return eventStoreRepository.findByAggregateId(aggregateId)
            .map(EventStore::getVersion)
            .orElse(0L);
    }
    
    @Override
    public void deleteEventStore(DeleteEventStoreCommand command) {
        log.warn("Deleting event store for aggregate: {}", command.getAggregateId());
        
        // Validate command
        command.validate();
        
        if (!command.isConfirmed()) {
            throw new IllegalArgumentException("Deletion must be confirmed");
        }
        
        try {
            // Check if event store exists
            Optional<EventStore> eventStore = eventStoreRepository.findByAggregateId(command.getAggregateId());
            
            if (eventStore.isEmpty()) {
                log.warn("No event store found for aggregate: {}", command.getAggregateId());
                return;
            }
            
            // Delete the event store
            eventStoreRepository.delete(eventStore.get());
            
            log.warn("Deleted event store for aggregate: {}", command.getAggregateId());
            
        } catch (Exception e) {
            log.error("Failed to delete event store for aggregate: {}", command.getAggregateId(), e);
            throw new EventStoreException("Failed to delete event store", e);
        }
    }
    
    // Helper methods
    
    private void publishEventsToStream(List<DomainEvent> events, String aggregateId) {
        try {
            // Group events by category for targeted publishing
            var eventsByCategory = events.stream()
                .collect(java.util.stream.Collectors.groupingBy(this::getEventCategory));
            
            // Publish events to appropriate streams
            eventsByCategory.forEach((category, categoryEvents) -> {
                String streamName = getStreamName(category);
                eventStreamPublisher.publish(streamName, categoryEvents);
            });
            
        } catch (Exception e) {
            log.error("Failed to publish events to stream for aggregate: {}", aggregateId, e);
            // Don't throw exception here - event storage should not fail due to streaming issues
        }
    }
    
    private EventCategory getEventCategory(DomainEvent event) {
        String eventType = event.getClass().getSimpleName();
        
        if (eventType.contains("Islamic") || eventType.contains("Sharia")) {
            return EventCategory.ISLAMIC_FINANCE;
        } else if (eventType.contains("Compliance") || eventType.contains("Regulatory")) {
            return EventCategory.REGULATORY;
        } else if (eventType.contains("Transfer") || eventType.contains("Payment")) {
            return EventCategory.PAYMENT;
        } else if (eventType.contains("Mint") || eventType.contains("Burn")) {
            return EventCategory.CBDC;
        } else if (eventType.contains("Customer")) {
            return EventCategory.CUSTOMER;
        } else {
            return EventCategory.GENERAL;
        }
    }
    
    private String getStreamName(EventCategory category) {
        return switch (category) {
            case ISLAMIC_FINANCE -> "amanahfi.islamic-finance";
            case REGULATORY -> "amanahfi.regulatory";
            case PAYMENT -> "amanahfi.payments";
            case CBDC -> "amanahfi.cbdc";
            case CUSTOMER -> "amanahfi.customers";
            case AUDIT -> "amanahfi.audit";
            case GENERAL -> "amanahfi.general";
        };
    }
}