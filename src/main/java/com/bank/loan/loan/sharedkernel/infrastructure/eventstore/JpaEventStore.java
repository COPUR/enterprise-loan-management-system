package com.bank.loanmanagement.loan.sharedkernel.infrastructure.eventstore;

import com.bank.loanmanagement.loan.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA-based Event Store implementation
 * Provides persistent event storage with ACID guarantees
 * Compatible with BIAN event sourcing patterns
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class JpaEventStore implements EventStore {

    private final EventStoreRepository eventStoreRepository;
    private final SnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveEvents(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        log.debug("Saving {} events for aggregate {}, expected version: {}", 
                 events.size(), aggregateId, expectedVersion);

        // Verify expected version for optimistic concurrency control
        Optional<Long> currentVersion = getLatestAggregateVersion(aggregateId);
        if (currentVersion.isPresent() && !currentVersion.get().equals(expectedVersion)) {
            throw new OptimisticLockingFailureException(
                String.format("Expected version %d but current version is %d for aggregate %s",
                             expectedVersion, currentVersion.get(), aggregateId));
        }

        // Convert and save events
        List<EventStoreEntry> entries = events.stream()
                .map(this::convertToEntry)
                .collect(Collectors.toList());

        eventStoreRepository.saveAll(entries);
        
        log.info("Successfully saved {} events for aggregate {}", events.size(), aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> getEventsForAggregate(String aggregateId) {
        log.debug("Retrieving all events for aggregate: {}", aggregateId);
        
        List<EventStoreEntry> entries = eventStoreRepository
                .findByAggregateIdOrderByVersionAsc(aggregateId);
        
        return entries.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> getEventsForAggregateFromVersion(String aggregateId, long fromVersion) {
        log.debug("Retrieving events for aggregate {} from version {}", aggregateId, fromVersion);
        
        List<EventStoreEntry> entries = eventStoreRepository
                .findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, fromVersion);
        
        return entries.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> getEventsByType(String eventType) {
        log.debug("Retrieving events by type: {}", eventType);
        
        List<EventStoreEntry> entries = eventStoreRepository
                .findByEventTypeOrderByOccurredOnAsc(eventType);
        
        return entries.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> getEventsByServiceDomain(String serviceDomain) {
        log.debug("Retrieving events by service domain: {}", serviceDomain);
        
        List<EventStoreEntry> entries = eventStoreRepository
                .findByServiceDomainOrderByOccurredOnAsc(serviceDomain);
        
        return entries.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> getLatestAggregateVersion(String aggregateId) {
        return eventStoreRepository.findMaxVersionByAggregateId(aggregateId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DomainEvent> getEventsAfter(OffsetDateTime timestamp) {
        log.debug("Retrieving events after timestamp: {}", timestamp);
        
        List<EventStoreEntry> entries = eventStoreRepository
                .findByOccurredOnAfterOrderByOccurredOnAsc(timestamp);
        
        return entries.stream()
                .map(this::convertToDomainEvent)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveSnapshot(String aggregateId, Object snapshot, long version) {
        log.debug("Saving snapshot for aggregate {} at version {}", aggregateId, version);
        
        try {
            String snapshotData = objectMapper.writeValueAsString(snapshot);
            
            SnapshotEntry entry = SnapshotEntry.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(snapshot.getClass().getSimpleName())
                    .snapshotData(snapshotData)
                    .version(version)
                    .createdAt(OffsetDateTime.now())
                    .build();
            
            snapshotRepository.save(entry);
            
            log.info("Successfully saved snapshot for aggregate {} at version {}", aggregateId, version);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize snapshot for aggregate " + aggregateId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AggregateSnapshot> getLatestSnapshot(String aggregateId) {
        log.debug("Retrieving latest snapshot for aggregate: {}", aggregateId);
        
        return snapshotRepository.findTopByAggregateIdOrderByVersionDesc(aggregateId)
                .map(entry -> {
                    try {
                        Object data = objectMapper.readValue(entry.getSnapshotData(), Object.class);
                        return new AggregateSnapshot(
                                entry.getAggregateId(),
                                entry.getAggregateType(),
                                data,
                                entry.getVersion(),
                                entry.getCreatedAt()
                        );
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to deserialize snapshot for aggregate " + aggregateId, e);
                    }
                });
    }

    @Override
    public boolean isHealthy() {
        try {
            eventStoreRepository.count();
            return true;
        } catch (Exception e) {
            log.error("Event store health check failed", e);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EventStoreStatistics getStatistics() {
        long totalEvents = eventStoreRepository.count();
        long totalAggregates = eventStoreRepository.countDistinctAggregates();
        long totalSnapshots = snapshotRepository.count();
        double avgEventsPerAggregate = totalAggregates > 0 ? (double) totalEvents / totalAggregates : 0;
        OffsetDateTime lastEventTime = eventStoreRepository.findLastEventTime()
                .orElse(OffsetDateTime.now());
        
        return new EventStoreStatistics(
                totalEvents,
                totalAggregates,
                totalSnapshots,
                avgEventsPerAggregate,
                lastEventTime
        );
    }

    private EventStoreEntry convertToEntry(DomainEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event.getEventData());
            
            return EventStoreEntry.builder()
                    .eventId(event.getEventId())
                    .aggregateId(event.getAggregateId())
                    .aggregateType(event.getAggregateType())
                    .eventType(event.getEventType())
                    .eventData(eventData)
                    .version(event.getVersion())
                    .occurredOn(event.getOccurredOn())
                    .serviceDomain(event.getServiceDomain())
                    .behaviorQualifier(event.getBehaviorQualifier())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event data for event " + event.getEventId(), e);
        }
    }

    private DomainEvent convertToDomainEvent(EventStoreEntry entry) {
        try {
            // In a real implementation, you would need a registry of event types
            // and their corresponding deserializers. For now, this is a simplified version.
            Class<?> eventClass = Class.forName(entry.getEventType());
            return (DomainEvent) objectMapper.readValue(entry.getEventData(), eventClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event " + entry.getEventId(), e);
        }
    }

    // JPA Entities for Event Store

    @Entity
    @Table(name = "event_store", indexes = {
            @Index(name = "idx_aggregate_id_version", columnList = "aggregateId,version"),
            @Index(name = "idx_event_type", columnList = "eventType"),
            @Index(name = "idx_service_domain", columnList = "serviceDomain"),
            @Index(name = "idx_occurred_on", columnList = "occurredOn")
    })
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EventStoreEntry {
        @Id
        private String eventId;
        
        @Column(nullable = false)
        private String aggregateId;
        
        @Column(nullable = false)
        private String aggregateType;
        
        @Column(nullable = false)
        private String eventType;
        
        @Lob
        @Column(nullable = false)
        private String eventData;
        
        @Column(nullable = false)
        private Long version;
        
        @Column(nullable = false)
        private OffsetDateTime occurredOn;
        
        private String serviceDomain;
        private String behaviorQualifier;
    }

    @Entity
    @Table(name = "aggregate_snapshots", indexes = {
            @Index(name = "idx_aggregate_id_version", columnList = "aggregateId,version")
    })
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SnapshotEntry {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(nullable = false)
        private String aggregateId;
        
        @Column(nullable = false)
        private String aggregateType;
        
        @Lob
        @Column(nullable = false)
        private String snapshotData;
        
        @Column(nullable = false)
        private Long version;
        
        @Column(nullable = false)
        private OffsetDateTime createdAt;
    }

    // Repository interfaces
    @Repository
    public interface EventStoreRepository extends JpaRepository<EventStoreEntry, String> {
        List<EventStoreEntry> findByAggregateIdOrderByVersionAsc(String aggregateId);
        List<EventStoreEntry> findByAggregateIdAndVersionGreaterThanOrderByVersionAsc(String aggregateId, Long version);
        List<EventStoreEntry> findByEventTypeOrderByOccurredOnAsc(String eventType);
        List<EventStoreEntry> findByServiceDomainOrderByOccurredOnAsc(String serviceDomain);
        List<EventStoreEntry> findByOccurredOnAfterOrderByOccurredOnAsc(OffsetDateTime timestamp);
        
        @Query("SELECT MAX(e.version) FROM EventStoreEntry e WHERE e.aggregateId = :aggregateId")
        Optional<Long> findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);
        
        @Query("SELECT COUNT(DISTINCT e.aggregateId) FROM EventStoreEntry e")
        long countDistinctAggregates();
        
        @Query("SELECT MAX(e.occurredOn) FROM EventStoreEntry e")
        Optional<OffsetDateTime> findLastEventTime();
    }

    @Repository
    public interface SnapshotRepository extends JpaRepository<SnapshotEntry, Long> {
        Optional<SnapshotEntry> findTopByAggregateIdOrderByVersionDesc(String aggregateId);
    }
}