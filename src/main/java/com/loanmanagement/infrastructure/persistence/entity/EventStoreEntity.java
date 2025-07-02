// infrastructure/persistence/entity/EventStoreEntity.java
package com.loanmanagement.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * Event Store Entity for Event Sourcing following EDA and hexagonal architecture principles.
 * This class serves as an infrastructure adapter for persisting domain events.
 *
 * Follows EDA principles by:
 * - Capturing all domain events in an immutable event stream
 * - Maintaining event ordering through versioning
 * - Supporting event replay and audit capabilities
 *
 * Supports DDD principles by:
 * - Acting as an infrastructure concern separate from domain logic
 * - Enabling event sourcing patterns for aggregate reconstruction
 * - Maintaining strong consistency within aggregate boundaries
 *
 * Follows 12-Factor App principles by:
 * - Treating logs (events) as event streams
 * - Supporting stateless application design through event persistence
 *
 * Note: This is a fallback implementation while Jakarta Persistence dependencies are resolved.
 * To restore full JPA functionality:
 * 1. Run: ./gradlew clean build --refresh-dependencies
 * 2. Verify Jakarta Persistence and Spring Data JPA dependencies
 * 3. Add proper JPA annotations once dependencies are available
 */
public class EventStoreEntity {

    private String eventId;
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    private String eventData;
    private Instant occurredOn;
    private Long version;

    /**
     * Default constructor required for JPA and serialization frameworks.
     */
    public EventStoreEntity() {
        this.occurredOn = Instant.now();
    }

    /**
     * Constructor for creating new event store entries.
     * Follows clean code principles with clear parameter validation.
     */
    public EventStoreEntity(String eventId, String aggregateId, String aggregateType,
                           String eventType, String eventData, Long version) {
        this();
        validateEventCreation(eventId, aggregateId, aggregateType, eventType, eventData, version);

        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.eventData = eventData;
        this.version = version;
    }

    /**
     * Factory method for creating event store entities from domain events.
     * Follows DDD principles by providing clear conversion from domain to infrastructure.
     */
    public static EventStoreEntity fromDomainEvent(String eventId, String aggregateId,
                                                  String aggregateType, String eventType,
                                                  String serializedData, Long version) {
        return new EventStoreEntity(eventId, aggregateId, aggregateType,
                                   eventType, serializedData, version);
    }

    /**
     * Validates event creation parameters following clean code principles.
     * Private method to ensure data integrity at construction time.
     */
    private void validateEventCreation(String eventId, String aggregateId, String aggregateType,
                                     String eventType, String eventData, Long version) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or empty");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (eventData == null) {
            throw new IllegalArgumentException("Event data cannot be null");
        }
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
    }

    /**
     * Returns true if this event occurred before the specified event.
     * Business logic method supporting EDA event ordering.
     */
    public boolean occurredBefore(EventStoreEntity other) {
        if (other == null) return false;
        return this.occurredOn.isBefore(other.occurredOn);
    }

    /**
     * Returns true if this event belongs to the same aggregate as the specified event.
     * Business logic method supporting DDD aggregate boundaries.
     */
    public boolean belongsToSameAggregate(EventStoreEntity other) {
        if (other == null) return false;
        return Objects.equals(this.aggregateId, other.aggregateId) &&
               Objects.equals(this.aggregateType, other.aggregateType);
    }

    /**
     * Returns true if this is the first event for the aggregate (version 0).
     * Supporting EDA event stream analysis.
     */
    public boolean isFirstEvent() {
        return version != null && version == 0L;
    }

    /**
     * Returns a formatted string representation for logging and debugging.
     * Follows clean code principles with meaningful output.
     */
    public String getEventSummary() {
        return String.format("Event[id=%s, type=%s, aggregate=%s:%s, version=%d, occurred=%s]",
                eventId, eventType, aggregateType, aggregateId, version, occurredOn);
    }

    // Getters and setters with proper encapsulation
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }
        this.eventId = eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        if (aggregateId == null || aggregateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate ID cannot be null or empty");
        }
        this.aggregateId = aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new IllegalArgumentException("Aggregate type cannot be null or empty");
        }
        this.aggregateType = aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        this.eventType = eventType;
    }

    public String getEventData() {
        return eventData;
    }

    public void setEventData(String eventData) {
        if (eventData == null) {
            throw new IllegalArgumentException("Event data cannot be null");
        }
        this.eventData = eventData;
    }

    public Instant getOccurredOn() {
        return occurredOn;
    }

    public void setOccurredOn(Instant occurredOn) {
        if (occurredOn == null) {
            throw new IllegalArgumentException("Occurred on timestamp cannot be null");
        }
        this.occurredOn = occurredOn;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        if (version == null || version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
        this.version = version;
    }

    // Equals and hashCode based on event identity (eventId)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStoreEntity that = (EventStoreEntity) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "EventStoreEntity{" +
                "eventId='" + eventId + '\'' +
                ", aggregateId='" + aggregateId + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", eventType='" + eventType + '\'' +
                ", occurredOn=" + occurredOn +
                ", version=" + version +
                '}';
    }
}

/*
 * JPA AND SPRING DATA RESTORATION GUIDE:
 *
 * Once Jakarta Persistence and Spring Data dependencies are resolved:
 *
 * @Entity
 * @Table(name = "event_store", indexes = {
 *     @Index(name = "idx_aggregate_id_version", columnList = "aggregateId,version", unique = true),
 *     @Index(name = "idx_occurred_on", columnList = "occurredOn"),
 *     @Index(name = "idx_event_type", columnList = "eventType"),
 *     @Index(name = "idx_aggregate_type", columnList = "aggregateType")
 * })
 * public class EventStoreEntity {
 *
 *     @Id
 *     @Column(length = 36)
 *     private String eventId;
 *
 *     @Column(name = "aggregate_id", nullable = false, length = 36)
 *     private String aggregateId;
 *
 *     @Column(name = "aggregate_type", nullable = false, length = 100)
 *     private String aggregateType;
 *
 *     @Column(name = "event_type", nullable = false, length = 100)
 *     private String eventType;
 *
 *     @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
 *     private String eventData;
 *
 *     @Column(name = "occurred_on", nullable = false)
 *     private Instant occurredOn;
 *
 *     @Column(nullable = false)
 *     private Long version;
 * }
 *
 * ARCHITECTURAL BENEFITS:
 * - 12-Factor: Events as immutable logs, stateless application design
 * - DDD: Clean separation between domain events and infrastructure persistence
 * - Hexagonal: Infrastructure adapter for event persistence without domain coupling
 * - EDA: Complete event sourcing support with versioning and ordering
 * - Clean Code: Validation, meaningful methods, comprehensive documentation
 */
