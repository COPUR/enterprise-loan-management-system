package com.bank.infrastructure.eventsourcing;

import com.bank.shared.kernel.domain.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * High-Performance Event Serializer
 * 
 * Optimized serialization for domain events with:
 * - Binary serialization for performance
 * - Compression for storage efficiency
 * - Schema evolution support
 * - Metrics integration
 * - Type safety and validation
 */
@Component
public class EventSerializer {
    
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Map<String, Class<? extends DomainEvent>> eventTypeRegistry = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final Timer serializationTimer;
    private final Timer deserializationTimer;
    private final Timer compressionTimer;
    
    public EventSerializer(ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.serializationTimer = Timer.builder("event.serialization")
            .description("Time taken to serialize events")
            .register(meterRegistry);
        this.deserializationTimer = Timer.builder("event.deserialization")
            .description("Time taken to deserialize events")
            .register(meterRegistry);
        this.compressionTimer = Timer.builder("event.compression")
            .description("Time taken to compress/decompress events")
            .register(meterRegistry);
        
        initializeEventTypeRegistry();
    }
    
    /**
     * Serialize event to compressed binary format
     */
    public byte[] serialize(DomainEvent event) {
        return serializationTimer.recordCallable(() -> {
            try {
                // First serialize to JSON
                byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
                
                // Then compress
                return compressionTimer.recordCallable(() -> compress(jsonBytes));
            } catch (Exception e) {
                meterRegistry.counter("event.serialization.errors").increment();
                throw new EventSerializationException("Failed to serialize event: " + event.getEventId(), e);
            }
        });
    }
    
    /**
     * Serialize event metadata separately for indexing
     */
    public String serializeMetadata(DomainEvent event) {
        try {
            EventMetadata metadata = new EventMetadata(
                event.getEventId(),
                event.getClass().getSimpleName(),
                event.getOccurredOn(),
                event.getVersion(),
                event.getCorrelationId(),
                event.getCausationId(),
                extractBusinessMetadata(event)
            );
            
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            meterRegistry.counter("event.metadata.serialization.errors").increment();
            throw new EventSerializationException("Failed to serialize event metadata: " + event.getEventId(), e);
        }
    }
    
    /**
     * Deserialize event from binary format
     */
    public DomainEvent deserialize(byte[] eventData, String eventType, String metadata) {
        return deserializationTimer.recordCallable(() -> {
            try {
                // Decompress first
                byte[] jsonBytes = compressionTimer.recordCallable(() -> decompress(eventData));
                
                // Get event class
                Class<? extends DomainEvent> eventClass = getEventClass(eventType);
                
                // Deserialize
                return objectMapper.readValue(jsonBytes, eventClass);
            } catch (Exception e) {
                meterRegistry.counter("event.deserialization.errors").increment();
                throw new EventSerializationException("Failed to deserialize event of type: " + eventType, e);
            }
        });
    }
    
    /**
     * Check if event type is supported
     */
    public boolean isEventTypeSupported(String eventType) {
        return eventTypeRegistry.containsKey(eventType);
    }
    
    /**
     * Register new event type for schema evolution
     */
    public void registerEventType(String eventType, Class<? extends DomainEvent> eventClass) {
        eventTypeRegistry.put(eventType, eventClass);
        meterRegistry.counter("event.types.registered").increment();
    }
    
    /**
     * Get serialization statistics
     */
    public SerializationStats getStats() {
        return new SerializationStats(
            serializationTimer.count(),
            deserializationTimer.count(),
            serializationTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS),
            deserializationTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS),
            eventTypeRegistry.size()
        );
    }
    
    // Private helper methods
    
    private void initializeEventTypeRegistry() {
        // Register all known event types for the banking domain
        
        // Customer events
        registerEventType("CustomerCreatedEvent", loadEventClass("com.bank.customer.domain.CustomerCreatedEvent"));
        registerEventType("CustomerContactUpdatedEvent", loadEventClass("com.bank.customer.domain.CustomerContactUpdatedEvent"));
        registerEventType("CustomerCreditLimitUpdatedEvent", loadEventClass("com.bank.customer.domain.CustomerCreditLimitUpdatedEvent"));
        registerEventType("CustomerCreditReservedEvent", loadEventClass("com.bank.customer.domain.CustomerCreditReservedEvent"));
        registerEventType("CustomerCreditReleasedEvent", loadEventClass("com.bank.customer.domain.CustomerCreditReleasedEvent"));
        registerEventType("CustomerCreditScoreUpdatedEvent", loadEventClass("com.bank.customer.domain.CustomerCreditScoreUpdatedEvent"));
        
        // Loan events
        registerEventType("LoanCreatedEvent", loadEventClass("com.bank.loan.domain.LoanCreatedEvent"));
        registerEventType("LoanApprovedEvent", loadEventClass("com.bank.loan.domain.LoanApprovedEvent"));
        registerEventType("LoanRejectedEvent", loadEventClass("com.bank.loan.domain.LoanRejectedEvent"));
        registerEventType("LoanDisbursedEvent", loadEventClass("com.bank.loan.domain.LoanDisbursedEvent"));
        registerEventType("LoanPaymentMadeEvent", loadEventClass("com.bank.loan.domain.LoanPaymentMadeEvent"));
        registerEventType("LoanFullyPaidEvent", loadEventClass("com.bank.loan.domain.LoanFullyPaidEvent"));
        registerEventType("LoanDefaultedEvent", loadEventClass("com.bank.loan.domain.LoanDefaultedEvent"));
        
        // Payment events
        registerEventType("PaymentCreatedEvent", loadEventClass("com.bank.payment.domain.PaymentCreatedEvent"));
        registerEventType("PaymentProcessedEvent", loadEventClass("com.bank.payment.domain.PaymentProcessedEvent"));
        registerEventType("PaymentCompletedEvent", loadEventClass("com.bank.payment.domain.PaymentCompletedEvent"));
        registerEventType("PaymentFailedEvent", loadEventClass("com.bank.payment.domain.PaymentFailedEvent"));
        registerEventType("PaymentCancelledEvent", loadEventClass("com.bank.payment.domain.PaymentCancelledEvent"));
        
        // Islamic Banking events
        registerEventType("MurabahaContractCreatedEvent", loadEventClass("com.amanahfi.murabaha.domain.contract.MurabahaContractCreatedEvent"));
        registerEventType("ContractShariahApprovedEvent", loadEventClass("com.amanahfi.murabaha.domain.contract.ContractShariahApprovedEvent"));
        registerEventType("ContractActivatedEvent", loadEventClass("com.amanahfi.murabaha.domain.contract.ContractActivatedEvent"));
        registerEventType("InstallmentPaidEvent", loadEventClass("com.amanahfi.murabaha.domain.contract.InstallmentPaidEvent"));
        registerEventType("ContractEarlySettledEvent", loadEventClass("com.amanahfi.murabaha.domain.contract.ContractEarlySettledEvent"));
        registerEventType("ContractDefaultedEvent", loadEventClass("com.amanahfi.murabaha.domain.contract.ContractDefaultedEvent"));
        registerEventType("AssetDeliveredEvent", loadEventClass("com.amanahfi.murabaha.domain.contract.AssetDeliveredEvent"));
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends DomainEvent> loadEventClass(String className) {
        try {
            return (Class<? extends DomainEvent>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            // Return a default event class or create a placeholder
            return createPlaceholderEventClass(className);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends DomainEvent> createPlaceholderEventClass(String className) {
        // For events that don't exist yet, create a placeholder
        // This supports schema evolution and backward compatibility
        return (Class<? extends DomainEvent>) UnknownDomainEvent.class;
    }
    
    private Class<? extends DomainEvent> getEventClass(String eventType) {
        Class<? extends DomainEvent> eventClass = eventTypeRegistry.get(eventType);
        if (eventClass == null) {
            meterRegistry.counter("event.unknown.types").increment();
            return UnknownDomainEvent.class;
        }
        return eventClass;
    }
    
    private byte[] compress(byte[] data) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data);
            gzipOut.finish();
            return baos.toByteArray();
        }
    }
    
    private byte[] decompress(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIn = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        }
    }
    
    private Map<String, Object> extractBusinessMetadata(DomainEvent event) {
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        
        // Extract business-specific metadata for indexing and searching
        String eventType = event.getClass().getSimpleName();
        
        if (eventType.contains("Customer")) {
            metadata.put("context", "customer");
        } else if (eventType.contains("Loan")) {
            metadata.put("context", "loan");
        } else if (eventType.contains("Payment")) {
            metadata.put("context", "payment");
        } else if (eventType.contains("Murabaha")) {
            metadata.put("context", "islamic-banking");
            metadata.put("compliance", "sharia");
        }
        
        // Add regulatory flags
        if (eventType.contains("Approved") || eventType.contains("Rejected")) {
            metadata.put("requiresAudit", true);
        }
        
        if (eventType.contains("Payment") || eventType.contains("Disburse")) {
            metadata.put("financialTransaction", true);
        }
        
        return metadata;
    }
    
    /**
     * Event metadata for efficient querying
     */
    public static class EventMetadata {
        private final String eventId;
        private final String eventType;
        private final java.time.Instant occurredOn;
        private final Long version;
        private final String correlationId;
        private final String causationId;
        private final Map<String, Object> businessMetadata;
        
        public EventMetadata(String eventId, String eventType, java.time.Instant occurredOn, 
                           Long version, String correlationId, String causationId,
                           Map<String, Object> businessMetadata) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.occurredOn = occurredOn;
            this.version = version;
            this.correlationId = correlationId;
            this.causationId = causationId;
            this.businessMetadata = businessMetadata;
        }
        
        // Getters
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public java.time.Instant getOccurredOn() { return occurredOn; }
        public Long getVersion() { return version; }
        public String getCorrelationId() { return correlationId; }
        public String getCausationId() { return causationId; }
        public Map<String, Object> getBusinessMetadata() { return businessMetadata; }
    }
    
    /**
     * Serialization performance statistics
     */
    public static class SerializationStats {
        private final long serializationCount;
        private final long deserializationCount;
        private final double totalSerializationTimeMs;
        private final double totalDeserializationTimeMs;
        private final int registeredEventTypes;
        
        public SerializationStats(long serializationCount, long deserializationCount,
                                double totalSerializationTimeMs, double totalDeserializationTimeMs,
                                int registeredEventTypes) {
            this.serializationCount = serializationCount;
            this.deserializationCount = deserializationCount;
            this.totalSerializationTimeMs = totalSerializationTimeMs;
            this.totalDeserializationTimeMs = totalDeserializationTimeMs;
            this.registeredEventTypes = registeredEventTypes;
        }
        
        public double getAverageSerializationTimeMs() {
            return serializationCount > 0 ? totalSerializationTimeMs / serializationCount : 0;
        }
        
        public double getAverageDeserializationTimeMs() {
            return deserializationCount > 0 ? totalDeserializationTimeMs / deserializationCount : 0;
        }
        
        // Getters
        public long getSerializationCount() { return serializationCount; }
        public long getDeserializationCount() { return deserializationCount; }
        public double getTotalSerializationTimeMs() { return totalSerializationTimeMs; }
        public double getTotalDeserializationTimeMs() { return totalDeserializationTimeMs; }
        public int getRegisteredEventTypes() { return registeredEventTypes; }
    }
    
    /**
     * Placeholder for unknown event types (schema evolution support)
     */
    public static class UnknownDomainEvent implements DomainEvent {
        private String eventId = "unknown-" + java.util.UUID.randomUUID();
        private java.time.Instant occurredOn = java.time.Instant.now();
        private Long version = 1L;
        
        @Override
        public String getEventId() { return eventId; }
        
        @Override
        public java.time.Instant getOccurredOn() { return occurredOn; }
        
        @Override
        public Long getVersion() { return version; }
        
        @Override
        public String getAggregateId() { return "unknown"; }
        
        @Override
        public String getCorrelationId() { return null; }
        
        @Override
        public String getCausationId() { return null; }
    }
    
    /**
     * Exception for serialization errors
     */
    public static class EventSerializationException extends RuntimeException {
        public EventSerializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}