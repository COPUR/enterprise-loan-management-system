// infrastructure/persistence/entity/EventStoreEntity.java
package com.loanmanagement.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

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
    
    // Constructors, getters, setters
}


interface SpringDataEventStoreRepository extends JpaRepository<EventStoreEntity, String> {
    List<EventStoreEntity> findByAggregateIdOrderByVersion(String aggregateId);
}