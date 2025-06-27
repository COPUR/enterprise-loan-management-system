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
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.LoanApplicationSubmittedEvent.class, name = "LoanApplicationSubmitted"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.LoanApprovedEvent.class, name = "LoanApproved"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.LoanRejectedEvent.class, name = "LoanRejected"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.PaymentProcessedEvent.class, name = "PaymentProcessed"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.FraudDetectedEvent.class, name = "FraudDetected"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.AIRecommendationGeneratedEvent.class, name = "AIRecommendationGenerated"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.ComplianceCheckCompletedEvent.class, name = "ComplianceCheckCompleted"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.SagaStartedEvent.class, name = "SagaStarted"),
    @JsonSubTypes.Type(value = com.banking.loan.domain.events.SagaFailedEvent.class, name = "SagaFailed")
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

