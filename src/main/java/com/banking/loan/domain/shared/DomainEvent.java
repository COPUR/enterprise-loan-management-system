package com.banking.loan.domain.shared;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

/**
 * Base domain event interface for event-driven architecture
 * All domain events in the banking system must implement this interface
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = LoanApplicationSubmittedEvent.class, name = "LoanApplicationSubmitted"),
    @JsonSubTypes.Type(value = LoanApprovedEvent.class, name = "LoanApproved"),
    @JsonSubTypes.Type(value = LoanRejectedEvent.class, name = "LoanRejected"),
    @JsonSubTypes.Type(value = PaymentProcessedEvent.class, name = "PaymentProcessed"),
    @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PaymentFailed"),
    @JsonSubTypes.Type(value = CustomerCreatedEvent.class, name = "CustomerCreated"),
    @JsonSubTypes.Type(value = FraudDetectedEvent.class, name = "FraudDetected"),
    @JsonSubTypes.Type(value = AIRecommendationGeneratedEvent.class, name = "AIRecommendationGenerated")
})
public interface DomainEvent {
    
    /**
     * Unique identifier for the event
     */
    UUID getEventId();
    
    /**
     * Type of the event (for routing and handling)
     */
    String getEventType();
    
    /**
     * Aggregate ID that this event relates to
     */
    String getAggregateId();
    
    /**
     * Version of the aggregate when this event occurred
     */
    Long getAggregateVersion();
    
    /**
     * Timestamp when the event occurred
     */
    Instant getOccurredOn();
    
    /**
     * User or system that triggered this event
     */
    String getTriggeredBy();
    
    /**
     * Correlation ID for distributed tracing
     */
    String getCorrelationId();
    
    /**
     * Tenant ID for multi-tenancy support
     */
    String getTenantId();
    
    /**
     * Event metadata for additional context
     */
    EventMetadata getMetadata();
    
    /**
     * Event schema version for event evolution
     */
    default String getSchemaVersion() {
        return "1.0";
    }
}

/**
 * Abstract base implementation of DomainEvent
 */
abstract class BaseDomainEvent implements DomainEvent {
    protected final UUID eventId;
    protected final String aggregateId;
    protected final Long aggregateVersion;
    protected final Instant occurredOn;
    protected final String triggeredBy;
    protected final String correlationId;
    protected final String tenantId;
    protected final EventMetadata metadata;
    
    protected BaseDomainEvent(String aggregateId, Long aggregateVersion, String triggeredBy, 
                            String correlationId, String tenantId, EventMetadata metadata) {
        this.eventId = UUID.randomUUID();
        this.aggregateId = aggregateId;
        this.aggregateVersion = aggregateVersion;
        this.occurredOn = Instant.now();
        this.triggeredBy = triggeredBy;
        this.correlationId = correlationId;
        this.tenantId = tenantId;
        this.metadata = metadata != null ? metadata : EventMetadata.empty();
    }
    
    @Override
    public UUID getEventId() { return eventId; }
    
    @Override
    public String getAggregateId() { return aggregateId; }
    
    @Override
    public Long getAggregateVersion() { return aggregateVersion; }
    
    @Override
    public Instant getOccurredOn() { return occurredOn; }
    
    @Override
    public String getTriggeredBy() { return triggeredBy; }
    
    @Override
    public String getCorrelationId() { return correlationId; }
    
    @Override
    public String getTenantId() { return tenantId; }
    
    @Override
    public EventMetadata getMetadata() { return metadata; }
}

/**
 * Event metadata for additional context
 */
public record EventMetadata(
    String source,
    String sourceVersion,
    String causationId,
    Map<String, Object> properties
) {
    public static EventMetadata empty() {
        return new EventMetadata(null, null, null, Collections.emptyMap());
    }
    
    public static EventMetadata of(String source, String sourceVersion) {
        return new EventMetadata(source, sourceVersion, null, Collections.emptyMap());
    }
    
    public static EventMetadata withProperties(String source, String sourceVersion, Map<String, Object> properties) {
        return new EventMetadata(source, sourceVersion, null, properties);
    }
}