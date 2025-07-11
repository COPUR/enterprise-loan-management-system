package com.amanahfi.platform.shared.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Base interface for all domain events in the AmanahFi Platform
 * 
 * Domain events represent important business occurrences that other parts
 * of the system might be interested in. This interface provides the common
 * structure for all events in the Islamic finance platform.
 * 
 * Key Principles:
 * - Events are immutable facts about what happened
 * - Events should be named in past tense (e.g., CustomerCreated, LoanApproved)
 * - Events contain all necessary data for interested parties
 * - Events support eventual consistency and event sourcing
 * 
 * Islamic Finance Context:
 * - Events can trigger Sharia compliance validations
 * - Support for regulatory reporting and audit trails
 * - Enable real-time risk assessment and monitoring
 * - Facilitate distributed transaction coordination (Saga pattern)
 * 
 * Polymorphic JSON Serialization:
 * This interface uses Jackson annotations to support polymorphic serialization
 * of events when they are stored in the event store or transmitted via Kafka.
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "eventType"
)
@JsonSubTypes({
    // Customer Management Events
    @JsonSubTypes.Type(value = Object.class, name = "CustomerCreated"),
    @JsonSubTypes.Type(value = Object.class, name = "CustomerActivated"),
    @JsonSubTypes.Type(value = Object.class, name = "CustomerSuspended"),
    @JsonSubTypes.Type(value = Object.class, name = "KycCompleted"),
    
    // Islamic Finance Product Events
    @JsonSubTypes.Type(value = Object.class, name = "MurabahaContractCreated"),
    @JsonSubTypes.Type(value = Object.class, name = "MusharakahPartnershipEstablished"),
    @JsonSubTypes.Type(value = Object.class, name = "IjarahLeaseInitiated"),
    @JsonSubTypes.Type(value = Object.class, name = "SalamContractExecuted"),
    @JsonSubTypes.Type(value = Object.class, name = "IstisnaProjectCommenced"),
    @JsonSubTypes.Type(value = Object.class, name = "QardHassanDisbursed"),
    
    // Payment and Settlement Events
    @JsonSubTypes.Type(value = Object.class, name = "PaymentInitiated"),
    @JsonSubTypes.Type(value = Object.class, name = "PaymentCompleted"),
    @JsonSubTypes.Type(value = Object.class, name = "PaymentFailed"),
    @JsonSubTypes.Type(value = Object.class, name = "CbdcTransactionExecuted"),
    
    // Compliance and Risk Events
    @JsonSubTypes.Type(value = Object.class, name = "ShariaComplianceValidated"),
    @JsonSubTypes.Type(value = Object.class, name = "RegulatoryReportGenerated"),
    @JsonSubTypes.Type(value = Object.class, name = "RiskAssessmentCompleted"),
    @JsonSubTypes.Type(value = Object.class, name = "FraudDetected"),
    
    // System Events
    @JsonSubTypes.Type(value = Object.class, name = "EventProcessingFailed"),
    @JsonSubTypes.Type(value = Object.class, name = "SystemHealthCheckCompleted")
})
public interface DomainEvent {

    /**
     * Gets the unique identifier for this event
     * 
     * Each event must have a unique identifier for tracking,
     * deduplication, and correlation purposes.
     * 
     * @return UUID representing the unique event identifier
     */
    UUID getEventId();

    /**
     * Gets the identifier of the aggregate that produced this event
     * 
     * This links the event back to the specific aggregate instance
     * that generated it, enabling event sourcing and aggregate
     * reconstruction.
     * 
     * @return The aggregate identifier as a string
     */
    String getAggregateId();

    /**
     * Gets the type of aggregate that produced this event
     * 
     * This helps in routing events to appropriate handlers
     * and organizing events by aggregate type.
     * 
     * @return The aggregate type name
     */
    String getAggregateType();

    /**
     * Gets the timestamp when this event occurred
     * 
     * Uses Instant for precise, timezone-independent timestamping.
     * This is crucial for audit trails and event ordering.
     * 
     * @return The instant when the event occurred
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant getOccurredOn();

    /**
     * Gets the version of the aggregate when this event was produced
     * 
     * This supports optimistic locking and ensures events are
     * applied in the correct order during aggregate reconstruction.
     * 
     * @return The aggregate version at the time of event creation
     */
    Long getAggregateVersion();

    /**
     * Gets the correlation ID for tracing related events
     * 
     * This enables distributed tracing and correlation of related
     * events across different aggregates and bounded contexts.
     * 
     * @return UUID for correlation, may be null for root events
     */
    UUID getCorrelationId();

    /**
     * Gets the causation ID that triggered this event
     * 
     * This links effects back to their causes, enabling
     * sophisticated event analysis and debugging.
     * 
     * @return UUID of the causing event, may be null for external triggers
     */
    UUID getCausationId();

    /**
     * Gets metadata associated with this event
     * 
     * Metadata can include information about the user who triggered
     * the event, the system context, compliance information, etc.
     * 
     * @return EventMetadata containing additional event information
     */
    EventMetadata getMetadata();

    /**
     * Default implementation for getting the event type name
     * 
     * This provides a consistent way to get the event type name
     * based on the class name, following the convention of
     * removing the "Event" suffix if present.
     * 
     * @return The event type name for serialization
     */
    default String getEventType() {
        String className = this.getClass().getSimpleName();
        return className.endsWith("Event") 
            ? className.substring(0, className.length() - 5)
            : className;
    }

    /**
     * Checks if this event is related to Sharia compliance
     * 
     * This method can be overridden by specific events to indicate
     * whether they require Sharia compliance processing.
     * 
     * @return true if the event requires Sharia compliance handling
     */
    default boolean requiresShariaCompliance() {
        return false;
    }

    /**
     * Checks if this event requires regulatory reporting
     * 
     * Some events must be reported to regulatory authorities
     * like CBUAE, VARA, or other MENAT region regulators.
     * 
     * @return true if the event requires regulatory reporting
     */
    default boolean requiresRegulatoryReporting() {
        return false;
    }

    /**
     * Gets the jurisdiction where this event occurred
     * 
     * This is important for multi-tenant deployments across
     * different countries with varying regulations.
     * 
     * @return ISO country code or jurisdiction identifier
     */
    default String getJurisdiction() {
        return getMetadata() != null ? getMetadata().getJurisdiction() : "AE";
    }
}