// infrastructure/persistence/entity/EventStoreEntity.java
package com.loanmanagement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "event_store", indexes = {
    @Index(name = "idx_aggregate_id", columnList = "aggregateId,version"),
    @Index(name = "idx_occurred_on", columnList = "occurredOn"),
    @Index(name = "idx_event_type", columnList = "eventType")
})
public class EventStoreEntity {
    @Id
    private String eventId;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false)
    private String aggregateType;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String eventData;
    
    @Column(nullable = false)
    private Instant occurredOn;
    
    @Column(nullable = false)
    private Long version;
    
    // Default constructor
    public EventStoreEntity() {}

    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }

    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventData() { return eventData; }
    public void setEventData(String eventData) { this.eventData = eventData; }

    public Instant getOccurredOn() { return occurredOn; }
    public void setOccurredOn(Instant occurredOn) { this.occurredOn = occurredOn; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}


interface SpringDataEventStoreRepository extends JpaRepository<EventStoreEntity, String> {
    List<EventStoreEntity> findByAggregateIdOrderByVersion(String aggregateId);
}