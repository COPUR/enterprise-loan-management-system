package com.amanahfi.platform.events.domain;

import com.amanahfi.platform.shared.domain.DomainEvent;
import com.amanahfi.platform.shared.domain.EventMetadata;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Stored event representation in the event store
 */
@Getter
@ToString
@Builder
public class StoredEvent {
    
    private final UUID eventId;
    private final String aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final String eventData;
    private final EventMetadata metadata;
    private final long version;
    private final Instant timestamp;
    private final String correlationId;
    private final String causationId;
    
    /**
     * Create a stored event from a domain event
     */
    public static StoredEvent fromDomainEvent(DomainEvent domainEvent, long version) {
        return StoredEvent.builder()
            .eventId(domainEvent.getEventId())
            .aggregateId(domainEvent.getAggregateId())
            .aggregateType(domainEvent.getAggregateType())
            .eventType(domainEvent.getClass().getSimpleName())
            .eventData(serializeEventData(domainEvent))
            .metadata(domainEvent.getMetadata())
            .version(version)
            .timestamp(domainEvent.getOccurredOn())
            .correlationId(domainEvent.getMetadata().getCorrelationId())
            .causationId(domainEvent.getMetadata().getCausationId())
            .build();
    }
    
    /**
     * Convert to domain event
     */
    public DomainEvent toDomainEvent() {
        return deserializeEventData(eventData, eventType);
    }
    
    /**
     * Check if this event requires Sharia compliance
     */
    public boolean requiresShariaCompliance() {
        // Check if event type is related to Islamic finance
        return eventType.contains("Islamic") || 
               eventType.contains("Sharia") || 
               eventType.contains("Murabaha") ||
               eventType.contains("Musharakah") ||
               eventType.contains("Ijarah") ||
               eventType.contains("QardHassan");
    }
    
    /**
     * Check if this event requires regulatory reporting
     */
    public boolean requiresRegulatoryReporting() {
        // Check if event type requires regulatory reporting
        return eventType.contains("Compliance") ||
               eventType.contains("Violation") ||
               eventType.contains("Transaction") ||
               eventType.contains("Mint") ||
               eventType.contains("Burn") ||
               eventType.contains("Freeze");
    }
    
    /**
     * Check if this event is a financial transaction
     */
    public boolean isFinancialTransaction() {
        return eventType.contains("Transfer") ||
               eventType.contains("Payment") ||
               eventType.contains("Mint") ||
               eventType.contains("Burn") ||
               eventType.contains("Received");
    }
    
    /**
     * Get event category for processing
     */
    public EventCategory getEventCategory() {
        if (eventType.contains("Islamic") || eventType.contains("Sharia")) {
            return EventCategory.ISLAMIC_FINANCE;
        } else if (eventType.contains("Compliance") || eventType.contains("Regulatory")) {
            return EventCategory.REGULATORY;
        } else if (eventType.contains("Transfer") || eventType.contains("Payment")) {
            return EventCategory.PAYMENT;
        } else if (eventType.contains("Mint") || eventType.contains("Burn")) {
            return EventCategory.CBDC;
        } else {
            return EventCategory.GENERAL;
        }
    }
    
    private static String serializeEventData(DomainEvent event) {
        // In a real implementation, this would use JSON serialization
        // For now, we'll use a simple string representation
        return event.toString();
    }
    
    private static DomainEvent deserializeEventData(String eventData, String eventType) {
        // In a real implementation, this would deserialize from JSON
        // For now, we'll return a placeholder
        throw new UnsupportedOperationException("Event deserialization not implemented");
    }
}