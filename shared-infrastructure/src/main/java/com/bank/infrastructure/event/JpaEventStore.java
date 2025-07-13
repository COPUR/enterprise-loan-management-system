package com.bank.infrastructure.event;

import com.bank.shared.kernel.domain.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JPA-based Event Store implementation
 * 
 * Provides persistent storage for domain events using JPA/Hibernate
 * Supports Event Sourcing and audit trail requirements
 */
@Component
@Transactional
public class JpaEventStore implements EventStore {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final ObjectMapper objectMapper;
    
    public JpaEventStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void store(DomainEvent event) {
        EventEntity entity = new EventEntity(event, objectMapper);
        entityManager.persist(entity);
    }
    
    @Override
    public void store(List<DomainEvent> events) {
        events.forEach(this::store);
    }
    
    @Override
    public List<DomainEvent> getEvents(String aggregateId) {
        List<EventEntity> entities = entityManager
            .createQuery("SELECT e FROM EventEntity e WHERE e.aggregateId = :aggregateId ORDER BY e.version", EventEntity.class)
            .setParameter("aggregateId", aggregateId)
            .getResultList();
        
        return entities.stream()
            .map(entity -> entity.toDomainEvent(objectMapper))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainEvent> getEventsFromVersion(String aggregateId, Long version) {
        List<EventEntity> entities = entityManager
            .createQuery("SELECT e FROM EventEntity e WHERE e.aggregateId = :aggregateId AND e.version >= :version ORDER BY e.version", EventEntity.class)
            .setParameter("aggregateId", aggregateId)
            .setParameter("version", version)
            .getResultList();
        
        return entities.stream()
            .map(entity -> entity.toDomainEvent(objectMapper))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainEvent> getEventsByType(Class<? extends DomainEvent> eventType) {
        List<EventEntity> entities = entityManager
            .createQuery("SELECT e FROM EventEntity e WHERE e.eventType = :eventType ORDER BY e.occurredOn", EventEntity.class)
            .setParameter("eventType", eventType.getSimpleName())
            .getResultList();
        
        return entities.stream()
            .map(entity -> entity.toDomainEvent(objectMapper))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainEvent> getEventsByTimeRange(Instant from, Instant to) {
        List<EventEntity> entities = entityManager
            .createQuery("SELECT e FROM EventEntity e WHERE e.occurredOn BETWEEN :from AND :to ORDER BY e.occurredOn", EventEntity.class)
            .setParameter("from", from)
            .setParameter("to", to)
            .getResultList();
        
        return entities.stream()
            .map(entity -> entity.toDomainEvent(objectMapper))
            .collect(Collectors.toList());
    }
    
    /**
     * JPA Entity for storing domain events
     */
    @Entity
    @Table(name = "domain_events")
    public static class EventEntity {
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        
        @Column(name = "event_id", nullable = false, unique = true)
        private String eventId;
        
        @Column(name = "aggregate_id", nullable = false)
        private String aggregateId;
        
        @Column(name = "event_type", nullable = false)
        private String eventType;
        
        @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
        private String eventData;
        
        @Column(name = "occurred_on", nullable = false)
        private Instant occurredOn;
        
        @Column(name = "version", nullable = false)
        private Long version;
        
        // JPA requires default constructor
        protected EventEntity() {}
        
        public EventEntity(DomainEvent event, ObjectMapper objectMapper) {
            this.eventId = event.getEventId();
            this.aggregateId = extractAggregateId(event);
            this.eventType = event.getClass().getSimpleName();
            this.occurredOn = event.getOccurredOn();
            this.version = 1L; // Default version
            
            try {
                this.eventData = objectMapper.writeValueAsString(event);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize event", e);
            }
        }
        
        public DomainEvent toDomainEvent(ObjectMapper objectMapper) {
            try {
                Class<?> eventClass = Class.forName("com.bank." + eventType.toLowerCase() + ".domain." + eventType);
                return (DomainEvent) objectMapper.readValue(eventData, eventClass);
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize event", e);
            }
        }
        
        private String extractAggregateId(DomainEvent event) {
            // This is a simplified implementation
            // In practice, you might want to use reflection or interfaces
            return event.getEventId(); // Placeholder
        }
        
        // Getters
        public Long getId() { return id; }
        public String getEventId() { return eventId; }
        public String getAggregateId() { return aggregateId; }
        public String getEventType() { return eventType; }
        public String getEventData() { return eventData; }
        public Instant getOccurredOn() { return occurredOn; }
        public Long getVersion() { return version; }
    }
}