package com.amanahfi.events.publisher;

import com.amanahfi.events.domain.DomainEvent;
import com.amanahfi.events.domain.EventMetadata;
import com.amanahfi.events.domain.IslamicBankingEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Event Metadata Enricher for Islamic Banking Compliance
 * 
 * Enriches domain events with:
 * - Islamic banking compliance metadata
 * - UAE regulatory compliance information
 * - Audit trail requirements
 * - Event correlation and causation tracking
 */
@Component
public class EventMetadataEnricher {

    /**
     * Enriches a domain event with compliance and audit metadata
     */
    public EventMetadata enrich(DomainEvent event) {
        EventMetadata.Builder builder = EventMetadata.builder()
            .eventId(generateEventId())
            .eventType(event.getEventType())
            .aggregateId(event.getAggregateId())
            .aggregateType(determineAggregateType(event))
            .version(1L)
            .timestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
            .correlationId(event.getCorrelationId() != null ? event.getCorrelationId() : generateCorrelationId());

        // Islamic Banking Compliance Enrichment
        if (event instanceof IslamicBankingEvent islamicEvent) {
            builder
                .islamicBankingCompliant(islamicEvent.isIslamicBankingCompliant())
                .regulatoryCompliance(islamicEvent.getRegulatoryCompliance())
                .auditRequired(true); // All Islamic banking events require audit

            // Sharia compliance for specific event types
            if (isShariahRelevantEvent(event.getEventType())) {
                builder.shariahApproved(!islamicEvent.requiresShariahBoardReview());
            }

            // CBDC compliance for payment events
            if (isCbdcRelevantEvent(event.getEventType())) {
                builder.cbdcCompliant(true);
            }

            // Compliance type based on event
            builder.complianceType(determineComplianceType(event.getEventType()));
        }

        // Audit level based on event criticality
        builder.auditLevel(determineAuditLevel(event.getEventType()));

        return builder.build();
    }

    /**
     * Determines the aggregate type from the event
     */
    private String determineAggregateType(DomainEvent event) {
        String eventType = event.getEventType();
        
        if (eventType.startsWith("Customer")) {
            return "Customer";
        } else if (eventType.startsWith("Account")) {
            return "Account";
        } else if (eventType.startsWith("Payment") || eventType.startsWith("Cbdc")) {
            return "Payment";
        } else if (eventType.startsWith("Murabaha")) {
            return "MurabahaContract";
        } else if (eventType.startsWith("Compliance")) {
            return "ComplianceCheck";
        } else {
            return "Unknown";
        }
    }

    /**
     * Determines if the event is relevant for Sharia compliance
     */
    private boolean isShariahRelevantEvent(String eventType) {
        return eventType.contains("Murabaha") || 
               eventType.contains("Contract") ||
               eventType.contains("Islamic") ||
               eventType.contains("Sharia");
    }

    /**
     * Determines if the event is relevant for CBDC compliance
     */
    private boolean isCbdcRelevantEvent(String eventType) {
        return eventType.contains("Cbdc") || 
               eventType.contains("DigitalDirham") ||
               eventType.contains("Payment");
    }

    /**
     * Determines compliance type based on event type
     */
    private String determineComplianceType(String eventType) {
        if (eventType.contains("Compliance")) {
            return "AML";
        } else if (isShariahRelevantEvent(eventType)) {
            return "SHARIA";
        } else if (isCbdcRelevantEvent(eventType)) {
            return "CBDC";
        } else {
            return "GENERAL";
        }
    }

    /**
     * Determines audit level based on event criticality
     */
    private String determineAuditLevel(String eventType) {
        // High criticality events
        if (eventType.contains("Payment") || 
            eventType.contains("Murabaha") ||
            eventType.contains("Compliance") ||
            eventType.contains("Fraud")) {
            return "HIGH";
        }
        
        // Medium criticality events
        if (eventType.contains("Customer") ||
            eventType.contains("Account") ||
            eventType.contains("Auth")) {
            return "MEDIUM";
        }
        
        // Low criticality events
        return "LOW";
    }

    /**
     * Generates a unique event ID
     */
    private String generateEventId() {
        return "EVT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Generates a correlation ID for event tracing
     */
    private String generateCorrelationId() {
        return "CORR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}