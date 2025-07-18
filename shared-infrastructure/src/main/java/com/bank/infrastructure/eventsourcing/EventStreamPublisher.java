package com.bank.infrastructure.eventsourcing;

import com.bank.shared.kernel.domain.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Event Stream Publisher for Real-time Event Distribution
 * 
 * Publishes domain events to Kafka for:
 * - Real-time projections and read models
 * - Inter-service communication
 * - Event-driven architecture support
 * - Islamic banking compliance streaming
 * - Audit and compliance requirements
 */
@Component
public class EventStreamPublisher {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    
    // Event handlers for local subscriptions
    private final Map<String, Consumer<DomainEvent>> eventHandlers = new ConcurrentHashMap<>();
    
    // Metrics
    private final Timer publishTimer;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter subscriptionCounter;
    
    // Topic configuration
    private static final String CUSTOMER_EVENTS_TOPIC = "banking.customer.events";
    private static final String LOAN_EVENTS_TOPIC = "banking.loan.events";
    private static final String PAYMENT_EVENTS_TOPIC = "banking.payment.events";
    private static final String ISLAMIC_BANKING_EVENTS_TOPIC = "amanahfi.islamic.events";
    private static final String AUDIT_EVENTS_TOPIC = "banking.audit.events";
    
    public EventStreamPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        
        // Initialize metrics
        this.publishTimer = Timer.builder("event.stream.publish")
            .description("Time taken to publish events to stream")
            .register(meterRegistry);
        this.publishSuccessCounter = Counter.builder("event.stream.publish.success")
            .description("Number of successfully published events")
            .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("event.stream.publish.failure")
            .description("Number of failed event publications")
            .register(meterRegistry);
        this.subscriptionCounter = Counter.builder("event.stream.subscriptions")
            .description("Number of active event subscriptions")
            .register(meterRegistry);
    }
    
    /**
     * Publish domain event to appropriate Kafka topic
     */
    public CompletableFuture<Void> publish(DomainEvent event) {
        return publishTimer.recordCallable(() -> {
            try {
                String topic = determineTopicForEvent(event);
                String eventKey = event.getAggregateId();
                String eventPayload = serializeEvent(event);
                
                // Create producer record with headers
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, eventKey, eventPayload);
                addEventHeaders(record, event);
                
                // Publish to Kafka
                CompletableFuture<SendResult<String, String>> kafkaFuture = kafkaTemplate.send(record);
                
                // Handle success/failure
                return kafkaFuture.handle((result, throwable) -> {
                    if (throwable != null) {
                        publishFailureCounter.increment();
                        handlePublishFailure(event, throwable);
                        throw new EventPublishException("Failed to publish event: " + event.getEventId(), throwable);
                    } else {
                        publishSuccessCounter.increment();
                        handlePublishSuccess(event, result);
                        
                        // Notify local subscribers
                        notifyLocalSubscribers(event);
                        
                        return null;
                    }
                });
            } catch (Exception e) {
                publishFailureCounter.increment();
                throw new EventPublishException("Failed to publish event: " + event.getEventId(), e);
            }
        });
    }
    
    /**
     * Subscribe to specific event type
     */
    public void subscribe(String eventType, EventHandler handler) {
        eventHandlers.put(eventType, handler::handle);
        subscriptionCounter.increment();
    }
    
    /**
     * Subscribe with consumer function
     */
    public void subscribe(String eventType, Consumer<DomainEvent> consumer) {
        eventHandlers.put(eventType, consumer);
        subscriptionCounter.increment();
    }
    
    /**
     * Unsubscribe from event type
     */
    public void unsubscribe(String eventType) {
        if (eventHandlers.remove(eventType) != null) {
            meterRegistry.counter("event.stream.unsubscriptions").increment();
        }
    }
    
    /**
     * Get streaming statistics
     */
    public StreamingStatistics getStatistics() {
        return new StreamingStatistics(
            publishSuccessCounter.count(),
            publishFailureCounter.count(),
            publishTimer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS),
            eventHandlers.size()
        );
    }
    
    // Private helper methods
    
    private String determineTopicForEvent(DomainEvent event) {
        String eventType = event.getClass().getSimpleName();
        
        // Customer events
        if (eventType.contains("Customer")) {
            return CUSTOMER_EVENTS_TOPIC;
        }
        
        // Loan events
        if (eventType.contains("Loan")) {
            return LOAN_EVENTS_TOPIC;
        }
        
        // Payment events
        if (eventType.contains("Payment")) {
            return PAYMENT_EVENTS_TOPIC;
        }
        
        // Islamic banking events
        if (eventType.contains("Murabaha") || eventType.contains("Contract") || 
            eventType.contains("Sharia") || eventType.contains("Islamic")) {
            return ISLAMIC_BANKING_EVENTS_TOPIC;
        }
        
        // Audit events (events that require regulatory compliance)
        if (eventType.contains("Approved") || eventType.contains("Rejected") || 
            eventType.contains("Defaulted") || eventType.contains("Failed")) {
            return AUDIT_EVENTS_TOPIC;
        }
        
        // Default to customer events
        return CUSTOMER_EVENTS_TOPIC;
    }
    
    private String serializeEvent(DomainEvent event) throws Exception {
        EventEnvelope envelope = new EventEnvelope(
            event.getEventId(),
            event.getClass().getSimpleName(),
            event.getAggregateId(),
            event.getVersion(),
            event.getOccurredOn(),
            event.getCorrelationId(),
            event.getCausationId(),
            objectMapper.writeValueAsString(event)
        );
        
        return objectMapper.writeValueAsString(envelope);
    }
    
    private void addEventHeaders(ProducerRecord<String, String> record, DomainEvent event) {
        // Add Kafka headers for routing and filtering
        record.headers().add("eventType", event.getClass().getSimpleName().getBytes());
        record.headers().add("aggregateId", event.getAggregateId().getBytes());
        record.headers().add("eventId", event.getEventId().getBytes());
        record.headers().add("version", event.getVersion().toString().getBytes());
        record.headers().add("occurredOn", event.getOccurredOn().toString().getBytes());
        
        if (event.getCorrelationId() != null) {
            record.headers().add("correlationId", event.getCorrelationId().getBytes());
        }
        
        if (event.getCausationId() != null) {
            record.headers().add("causationId", event.getCausationId().getBytes());
        }
        
        // Add business context headers
        addBusinessHeaders(record, event);
    }
    
    private void addBusinessHeaders(ProducerRecord<String, String> record, DomainEvent event) {
        String eventType = event.getClass().getSimpleName();
        
        // Banking context
        if (eventType.contains("Customer")) {
            record.headers().add("businessContext", "customer-management".getBytes());
        } else if (eventType.contains("Loan")) {
            record.headers().add("businessContext", "loan-management".getBytes());
        } else if (eventType.contains("Payment")) {
            record.headers().add("businessContext", "payment-processing".getBytes());
        }
        
        // Islamic banking compliance
        if (eventType.contains("Murabaha") || eventType.contains("Sharia")) {
            record.headers().add("islamicBanking", "true".getBytes());
            record.headers().add("complianceRequired", "sharia".getBytes());
        }
        
        // Financial transaction flag
        if (eventType.contains("Payment") || eventType.contains("Disburse") || 
            eventType.contains("Transfer") || eventType.contains("Deposit")) {
            record.headers().add("financialTransaction", "true".getBytes());
            record.headers().add("auditRequired", "true".getBytes());
        }
        
        // Regulatory compliance
        if (eventType.contains("Approved") || eventType.contains("Rejected") || 
            eventType.contains("Default") || eventType.contains("Fraud")) {
            record.headers().add("regulatoryEvent", "true".getBytes());
            record.headers().add("retentionPeriod", "7years".getBytes()); // Banking regulation requirement
        }
    }
    
    private void handlePublishSuccess(DomainEvent event, SendResult<String, String> result) {
        // Log successful publication
        System.out.println("Successfully published event " + event.getEventId() + 
                          " to topic " + result.getRecordMetadata().topic() + 
                          " partition " + result.getRecordMetadata().partition() + 
                          " offset " + result.getRecordMetadata().offset());
        
        // Update metrics with topic-specific counters
        meterRegistry.counter("event.stream.publish.success", 
                             "topic", result.getRecordMetadata().topic()).increment();
    }
    
    private void handlePublishFailure(DomainEvent event, Throwable throwable) {
        // Log failure
        System.err.println("Failed to publish event " + event.getEventId() + ": " + throwable.getMessage());
        
        // Update metrics
        meterRegistry.counter("event.stream.publish.failure", 
                             "eventType", event.getClass().getSimpleName()).increment();
        
        // Could implement dead letter queue or retry logic here
    }
    
    private void notifyLocalSubscribers(DomainEvent event) {
        String eventType = event.getClass().getSimpleName();
        Consumer<DomainEvent> handler = eventHandlers.get(eventType);
        
        if (handler != null) {
            try {
                handler.accept(event);
                meterRegistry.counter("event.stream.local.notifications.success").increment();
            } catch (Exception e) {
                meterRegistry.counter("event.stream.local.notifications.failure").increment();
                System.err.println("Failed to notify local subscriber for event " + event.getEventId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Event envelope for Kafka serialization
     */
    public static class EventEnvelope {
        private final String eventId;
        private final String eventType;
        private final String aggregateId;
        private final Long version;
        private final Instant occurredOn;
        private final String correlationId;
        private final String causationId;
        private final String payload;
        
        public EventEnvelope(String eventId, String eventType, String aggregateId, Long version,
                           Instant occurredOn, String correlationId, String causationId, String payload) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.aggregateId = aggregateId;
            this.version = version;
            this.occurredOn = occurredOn;
            this.correlationId = correlationId;
            this.causationId = causationId;
            this.payload = payload;
        }
        
        // Getters for JSON serialization
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public String getAggregateId() { return aggregateId; }
        public Long getVersion() { return version; }
        public Instant getOccurredOn() { return occurredOn; }
        public String getCorrelationId() { return correlationId; }
        public String getCausationId() { return causationId; }
        public String getPayload() { return payload; }
    }
    
    /**
     * Event handler interface
     */
    @FunctionalInterface
    public interface EventHandler {
        void handle(DomainEvent event);
    }
    
    /**
     * Streaming statistics
     */
    public static class StreamingStatistics {
        private final double successfulPublications;
        private final double failedPublications;
        private final double totalPublishTimeMs;
        private final int activeSubscriptions;
        
        public StreamingStatistics(double successfulPublications, double failedPublications,
                                 double totalPublishTimeMs, int activeSubscriptions) {
            this.successfulPublications = successfulPublications;
            this.failedPublications = failedPublications;
            this.totalPublishTimeMs = totalPublishTimeMs;
            this.activeSubscriptions = activeSubscriptions;
        }
        
        public double getSuccessRate() {
            double total = successfulPublications + failedPublications;
            return total > 0 ? (successfulPublications / total) * 100 : 0;
        }
        
        public double getAveragePublishTimeMs() {
            return successfulPublications > 0 ? totalPublishTimeMs / successfulPublications : 0;
        }
        
        // Getters
        public double getSuccessfulPublications() { return successfulPublications; }
        public double getFailedPublications() { return failedPublications; }
        public double getTotalPublishTimeMs() { return totalPublishTimeMs; }
        public int getActiveSubscriptions() { return activeSubscriptions; }
    }
    
    /**
     * Exception for event publishing failures
     */
    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}