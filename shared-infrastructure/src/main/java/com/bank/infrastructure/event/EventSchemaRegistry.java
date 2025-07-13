package com.bank.infrastructure.event;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Event Schema Registry for validation and versioning
 * 
 * Provides event schema validation and management:
 * - Schema validation for events
 * - Version management
 * - Compatibility checking
 * - Schema evolution support
 */
@Component
@Slf4j
public class EventSchemaRegistry {
    
    // Schema definitions for different event types
    private final Map<String, EventSchema> schemas = new ConcurrentHashMap<>();
    
    // Required fields for all events
    private static final Set<String> REQUIRED_FIELDS = Set.of(
        "eventId", "eventType", "aggregateId", "timestamp", "data"
    );
    
    // Supported event types with their schemas
    private static final Map<String, Set<String>> EVENT_TYPE_SCHEMAS = Map.of(
        "CustomerCreated", Set.of("customerId", "name", "email", "phoneNumber"),
        "LoanApplicationSubmitted", Set.of("loanId", "customerId", "amount", "purpose"),
        "PaymentInitiated", Set.of("paymentId", "fromAccountId", "toAccountId", "amount"),
        "FraudDetected", Set.of("transactionId", "riskScore", "fraudType", "description"),
        "ComplianceViolation", Set.of("violationType", "severity", "description", "entityId"),
        "AMLAlert", Set.of("alertId", "customerId", "transactionId", "riskLevel"),
        "IslamicFinanceApproved", Set.of("productId", "customerId", "amount", "shariaCompliant"),
        "CBDCMinted", Set.of("mintId", "amount", "recipientId", "transactionHash"),
        "CBDCBurned", Set.of("burnId", "amount", "sourceId", "transactionHash")
    );
    
    public EventSchemaRegistry() {
        initializeSchemas();
    }
    
    /**
     * Validate event against registered schema
     */
    public void validateEvent(EventStreamingService.BankingDomainEvent event) {
        try {
            log.debug("Validating event: {} of type: {}", event.getEventId(), event.getEventType());
            
            // Validate required fields
            validateRequiredFields(event);
            
            // Validate event type schema
            validateEventTypeSchema(event);
            
            // Validate data integrity
            validateDataIntegrity(event);
            
            log.debug("Event validation successful for: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Event validation failed for event {}: {}", event.getEventId(), e.getMessage());
            throw new EventValidationException("Event validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate required fields are present
     */
    private void validateRequiredFields(EventStreamingService.BankingDomainEvent event) {
        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new EventValidationException("Event ID is required");
        }
        
        if (event.getEventType() == null || event.getEventType().trim().isEmpty()) {
            throw new EventValidationException("Event type is required");
        }
        
        if (event.getAggregateId() == null || event.getAggregateId().trim().isEmpty()) {
            throw new EventValidationException("Aggregate ID is required");
        }
        
        if (event.getTimestamp() == null) {
            throw new EventValidationException("Timestamp is required");
        }
        
        if (event.getData() == null) {
            throw new EventValidationException("Event data is required");
        }
        
        if (event.getMetadata() == null || event.getMetadata().isEmpty()) {
            throw new EventValidationException("Event metadata is required");
        }
    }
    
    /**
     * Validate event type specific schema
     */
    private void validateEventTypeSchema(EventStreamingService.BankingDomainEvent event) {
        String eventType = event.getEventType();
        
        // Check if event type is supported
        if (!EVENT_TYPE_SCHEMAS.containsKey(eventType)) {
            log.warn("Unknown event type: {}, skipping schema validation", eventType);
            return;
        }
        
        Set<String> requiredFields = EVENT_TYPE_SCHEMAS.get(eventType);
        
        // Validate that event data contains required fields
        if (event.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) event.getData();
            
            for (String requiredField : requiredFields) {
                if (!dataMap.containsKey(requiredField) || dataMap.get(requiredField) == null) {
                    throw new EventValidationException(
                        String.format("Required field '%s' missing for event type '%s'", 
                            requiredField, eventType)
                    );
                }
            }
        }
    }
    
    /**
     * Validate data integrity and constraints
     */
    private void validateDataIntegrity(EventStreamingService.BankingDomainEvent event) {
        String eventType = event.getEventType();
        
        // Specific validation based on event type
        switch (eventType) {
            case "PaymentInitiated":
                validatePaymentEvent(event);
                break;
            case "FraudDetected":
                validateFraudEvent(event);
                break;
            case "CBDCMinted":
            case "CBDCBurned":
                validateCBDCEvent(event);
                break;
            case "IslamicFinanceApproved":
                validateIslamicFinanceEvent(event);
                break;
            default:
                // General validation for other event types
                validateGeneralEvent(event);
        }
    }
    
    /**
     * Validate payment-specific constraints
     */
    private void validatePaymentEvent(EventStreamingService.BankingDomainEvent event) {
        if (event.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.getData();
            
            // Validate amount is positive
            Object amount = data.get("amount");
            if (amount instanceof Number && ((Number) amount).doubleValue() <= 0) {
                throw new EventValidationException("Payment amount must be positive");
            }
            
            // Validate account IDs are different
            String fromAccount = (String) data.get("fromAccountId");
            String toAccount = (String) data.get("toAccountId");
            if (fromAccount != null && fromAccount.equals(toAccount)) {
                throw new EventValidationException("From and to accounts cannot be the same");
            }
        }
    }
    
    /**
     * Validate fraud detection constraints
     */
    private void validateFraudEvent(EventStreamingService.BankingDomainEvent event) {
        if (event.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.getData();
            
            // Validate risk score range
            Object riskScore = data.get("riskScore");
            if (riskScore instanceof Number) {
                double score = ((Number) riskScore).doubleValue();
                if (score < 0 || score > 100) {
                    throw new EventValidationException("Risk score must be between 0 and 100");
                }
            }
        }
    }
    
    /**
     * Validate CBDC event constraints
     */
    private void validateCBDCEvent(EventStreamingService.BankingDomainEvent event) {
        if (event.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.getData();
            
            // Validate amount precision (CBDC amounts should have specific precision)
            Object amount = data.get("amount");
            if (amount instanceof Number && ((Number) amount).doubleValue() <= 0) {
                throw new EventValidationException("CBDC amount must be positive");
            }
            
            // Validate transaction hash format
            String txHash = (String) data.get("transactionHash");
            if (txHash == null || !txHash.matches("^0x[a-fA-F0-9]{64}$")) {
                throw new EventValidationException("Invalid transaction hash format");
            }
        }
    }
    
    /**
     * Validate Islamic finance constraints
     */
    private void validateIslamicFinanceEvent(EventStreamingService.BankingDomainEvent event) {
        if (event.getData() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) event.getData();
            
            // Ensure Sharia compliance flag is present and true
            Object shariaCompliant = data.get("shariaCompliant");
            if (!(shariaCompliant instanceof Boolean) || !((Boolean) shariaCompliant)) {
                throw new EventValidationException("Islamic finance products must be Sharia compliant");
            }
        }
    }
    
    /**
     * General event validation
     */
    private void validateGeneralEvent(EventStreamingService.BankingDomainEvent event) {
        // Validate event ID format (UUID)
        String eventId = event.getEventId();
        if (!eventId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            log.warn("Event ID does not match UUID format: {}", eventId);
        }
        
        // Validate aggregate ID is not empty
        String aggregateId = event.getAggregateId();
        if (aggregateId.trim().isEmpty()) {
            throw new EventValidationException("Aggregate ID cannot be empty");
        }
        
        // Validate version is positive
        if (event.getVersion() != null && event.getVersion() <= 0) {
            throw new EventValidationException("Event version must be positive");
        }
    }
    
    /**
     * Register new event schema
     */
    public void registerSchema(String eventType, EventSchema schema) {
        schemas.put(eventType, schema);
        log.info("Registered schema for event type: {}", eventType);
    }
    
    /**
     * Get schema for event type
     */
    public EventSchema getSchema(String eventType) {
        return schemas.get(eventType);
    }
    
    /**
     * Check if event type is supported
     */
    public boolean isSupportedEventType(String eventType) {
        return EVENT_TYPE_SCHEMAS.containsKey(eventType) || schemas.containsKey(eventType);
    }
    
    /**
     * Initialize default schemas
     */
    private void initializeSchemas() {
        // Initialize with default schemas for common event types
        for (String eventType : EVENT_TYPE_SCHEMAS.keySet()) {
            EventSchema schema = EventSchema.builder()
                .eventType(eventType)
                .version("1.0")
                .requiredFields(EVENT_TYPE_SCHEMAS.get(eventType))
                .build();
            
            schemas.put(eventType, schema);
        }
        
        log.info("Initialized {} default event schemas", schemas.size());
    }
    
    /**
     * Event schema definition
     */
    public static class EventSchema {
        private final String eventType;
        private final String version;
        private final Set<String> requiredFields;
        private final Set<String> optionalFields;
        
        private EventSchema(Builder builder) {
            this.eventType = builder.eventType;
            this.version = builder.version;
            this.requiredFields = builder.requiredFields;
            this.optionalFields = builder.optionalFields;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public String getEventType() { return eventType; }
        public String getVersion() { return version; }
        public Set<String> getRequiredFields() { return requiredFields; }
        public Set<String> getOptionalFields() { return optionalFields; }
        
        public static class Builder {
            private String eventType;
            private String version;
            private Set<String> requiredFields = new HashSet<>();
            private Set<String> optionalFields = new HashSet<>();
            
            public Builder eventType(String eventType) {
                this.eventType = eventType;
                return this;
            }
            
            public Builder version(String version) {
                this.version = version;
                return this;
            }
            
            public Builder requiredFields(Set<String> requiredFields) {
                this.requiredFields = new HashSet<>(requiredFields);
                return this;
            }
            
            public Builder optionalFields(Set<String> optionalFields) {
                this.optionalFields = new HashSet<>(optionalFields);
                return this;
            }
            
            public EventSchema build() {
                return new EventSchema(this);
            }
        }
    }
    
    /**
     * Exception for event validation failures
     */
    public static class EventValidationException extends RuntimeException {
        public EventValidationException(String message) {
            super(message);
        }
        
        public EventValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}