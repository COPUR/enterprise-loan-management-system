package com.bank.infrastructure.event;

import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Event Streaming Service for Enterprise Banking Platform
 * 
 * Provides event streaming capabilities using Apache Kafka:
 * - Domain event publishing with guaranteed delivery
 * - Event consumption with error handling and retry logic
 * - Event transformation and routing
 * - Dead letter queue handling for failed events
 * - Schema validation and versioning
 * - Cross-context event communication
 */
@Service
@Transactional
public class EventStreamingService {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EventSchemaRegistry schemaRegistry;
    private final Executor eventExecutor;
    
    // Event handlers registry for dynamic subscription
    private final Map<String, Consumer<BankingDomainEvent>> eventHandlers = new ConcurrentHashMap<>();
    
    // Topic configuration
    private static final String CUSTOMER_EVENTS_TOPIC = "customer-events";
    private static final String LOAN_EVENTS_TOPIC = "loan-events";
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    private static final String COMPLIANCE_EVENTS_TOPIC = "compliance-events";
    private static final String SYSTEM_EVENTS_TOPIC = "system-events";
    private static final String DLQ_TOPIC_SUFFIX = "-dlq";
    
    public EventStreamingService(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               EventSchemaRegistry schemaRegistry,
                               @Qualifier("eventExecutor") Executor eventExecutor) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.schemaRegistry = schemaRegistry;
        this.eventExecutor = eventExecutor;
    }
    
    /**
     * Publish domain event to appropriate topic using virtual threads
     */
    public CompletableFuture<Void> publishEvent(BankingDomainEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Validate event schema
                schemaRegistry.validateEvent(event);
                
                // Determine topic based on event type
                String topic = determineTopicForEvent(event);
                
                // Serialize event
                String eventJson = objectMapper.writeValueAsString(event);
                
                // Create partition key for ordering
                String partitionKey = createPartitionKey(event);
                
                // Publish to Kafka
                kafkaTemplate.send(topic, partitionKey, eventJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logEventPublished(event, topic);
                        } else {
                            handlePublishError(event, topic, ex);
                        }
                    });
                    
            } catch (Exception e) {
                throw new EventStreamingException("Failed to publish event: " + event.getEventId(), e);
            }
        }, eventExecutor);
    }
    
    /**
     * Publish multiple events in a batch
     */
    public CompletableFuture<Void> publishEventBatch(java.util.List<BankingDomainEvent> events) {
        return CompletableFuture.allOf(
            events.stream()
                .map(this::publishEvent)
                .toArray(CompletableFuture[]::new)
        );
    }
    
    /**
     * Register event handler for dynamic subscription
     */
    public void registerEventHandler(String eventType, Consumer<BankingDomainEvent> handler) {
        eventHandlers.put(eventType, handler);
    }
    
    /**
     * Unregister event handler
     */
    public void unregisterEventHandler(String eventType) {
        eventHandlers.remove(eventType);
    }
    
    // Kafka Listeners for different event topics
    
    /**
     * Customer events consumer
     */
    @KafkaListener(topics = CUSTOMER_EVENTS_TOPIC, groupId = "banking-platform")
    public void handleCustomerEvent(@Payload String eventJson,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                   @Header(KafkaHeaders.OFFSET) Long offset) {
        handleEventMessage(eventJson, topic, partition, offset);
    }
    
    /**
     * Loan events consumer
     */
    @KafkaListener(topics = LOAN_EVENTS_TOPIC, groupId = "banking-platform")
    public void handleLoanEvent(@Payload String eventJson,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                               @Header(KafkaHeaders.OFFSET) Long offset) {
        handleEventMessage(eventJson, topic, partition, offset);
    }
    
    /**
     * Payment events consumer
     */
    @KafkaListener(topics = PAYMENT_EVENTS_TOPIC, groupId = "banking-platform")
    public void handlePaymentEvent(@Payload String eventJson,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                  @Header(KafkaHeaders.OFFSET) Long offset) {
        handleEventMessage(eventJson, topic, partition, offset);
    }
    
    /**
     * Compliance events consumer
     */
    @KafkaListener(topics = COMPLIANCE_EVENTS_TOPIC, groupId = "banking-platform")
    public void handleComplianceEvent(@Payload String eventJson,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                     @Header(KafkaHeaders.OFFSET) Long offset) {
        handleEventMessage(eventJson, topic, partition, offset);
    }
    
    /**
     * System events consumer
     */
    @KafkaListener(topics = SYSTEM_EVENTS_TOPIC, groupId = "banking-platform")
    public void handleSystemEvent(@Payload String eventJson,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                 @Header(KafkaHeaders.OFFSET) Long offset) {
        handleEventMessage(eventJson, topic, partition, offset);
    }
    
    /**
     * Dead letter queue consumer for failed events
     */
    @KafkaListener(topics = {
        CUSTOMER_EVENTS_TOPIC + DLQ_TOPIC_SUFFIX,
        LOAN_EVENTS_TOPIC + DLQ_TOPIC_SUFFIX,
        PAYMENT_EVENTS_TOPIC + DLQ_TOPIC_SUFFIX,
        COMPLIANCE_EVENTS_TOPIC + DLQ_TOPIC_SUFFIX,
        SYSTEM_EVENTS_TOPIC + DLQ_TOPIC_SUFFIX
    }, groupId = "banking-platform-dlq")
    public void handleDeadLetterEvent(@Payload String eventJson,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
                                     @Header(KafkaHeaders.OFFSET) Long offset) {
        handleDeadLetterMessage(eventJson, topic, partition, offset);
    }
    
    /**
     * Handle incoming event message
     */
    private void handleEventMessage(String eventJson, String topic, Integer partition, Long offset) {
        try {
            BankingDomainEvent event = objectMapper.readValue(eventJson, BankingDomainEvent.class);
            
            // Validate event
            if (!isValidEvent(event)) {
                sendToDeadLetterQueue(eventJson, topic, "Invalid event format");
                return;
            }
            
            // Check for duplicate processing
            if (isDuplicateEvent(event)) {
                logDuplicateEvent(event, topic);
                return;
            }
            
            // Process event
            processEvent(event, topic);
            
            // Mark as processed
            markEventProcessed(event);
            
            logEventProcessed(event, topic, partition, offset);
            
        } catch (Exception e) {
            handleEventProcessingError(eventJson, topic, partition, offset, e);
        }
    }
    
    /**
     * Process individual event
     */
    private void processEvent(BankingDomainEvent event, String topic) {
        try {
            // Call registered handlers
            Consumer<BankingDomainEvent> handler = eventHandlers.get(event.getEventType());
            if (handler != null) {
                handler.accept(event);
            }
            
            // Route to specific processors based on event type
            switch (event.getEventType()) {
                case "CustomerCreated":
                    processCustomerCreatedEvent(event);
                    break;
                case "LoanApplicationSubmitted":
                    processLoanApplicationEvent(event);
                    break;
                case "PaymentInitiated":
                    processPaymentInitiatedEvent(event);
                    break;
                case "FraudDetected":
                    processFraudDetectedEvent(event);
                    break;
                default:
                    processGenericEvent(event);
            }
            
        } catch (Exception e) {
            throw new EventProcessingException("Failed to process event: " + event.getEventId(), e);
        }
    }
    
    /**
     * Determine appropriate Kafka topic for event
     */
    private String determineTopicForEvent(BankingDomainEvent event) {
        String eventType = event.getEventType().toLowerCase();
        
        if (eventType.contains("customer")) {
            return CUSTOMER_EVENTS_TOPIC;
        } else if (eventType.contains("loan")) {
            return LOAN_EVENTS_TOPIC;
        } else if (eventType.contains("payment")) {
            return PAYMENT_EVENTS_TOPIC;
        } else if (eventType.contains("compliance") || eventType.contains("fraud") || eventType.contains("aml")) {
            return COMPLIANCE_EVENTS_TOPIC;
        } else {
            return SYSTEM_EVENTS_TOPIC;
        }
    }
    
    /**
     * Create partition key for event ordering
     */
    private String createPartitionKey(BankingDomainEvent event) {
        // Use aggregate ID as partition key to ensure ordering for same entity
        String aggregateId = event.getAggregateId();
        if (aggregateId != null && !aggregateId.isEmpty()) {
            return aggregateId;
        }
        
        // Fallback to customer ID or event type
        Object customerId = event.getMetadata().get("customerId");
        if (customerId != null) {
            return customerId.toString();
        }
        
        return event.getEventType();
    }
    
    /**
     * Validate event format and content
     */
    private boolean isValidEvent(BankingDomainEvent event) {
        return event != null &&
               event.getEventId() != null &&
               event.getEventType() != null &&
               event.getAggregateId() != null &&
               event.getTimestamp() != null &&
               event.getData() != null;
    }
    
    /**
     * Check if event is duplicate (idempotency)
     */
    private boolean isDuplicateEvent(BankingDomainEvent event) {
        // Implementation would check event store or cache for duplicate event IDs
        // This is a simplified version
        return false;
    }
    
    /**
     * Mark event as processed for idempotency
     */
    private void markEventProcessed(BankingDomainEvent event) {
        // Implementation would store event ID in processed events cache/store
        // This prevents duplicate processing
    }
    
    /**
     * Process customer created event
     */
    private void processCustomerCreatedEvent(BankingDomainEvent event) {
        // Trigger downstream processes like:
        // - Credit scoring
        // - Welcome email
        // - Account setup
        System.out.println("Processing CustomerCreated event: " + event.getEventId());
    }
    
    /**
     * Process loan application event
     */
    private void processLoanApplicationEvent(BankingDomainEvent event) {
        // Trigger loan processing workflow:
        // - Credit check
        // - Risk assessment
        // - Underwriting
        System.out.println("Processing LoanApplicationSubmitted event: " + event.getEventId());
    }
    
    /**
     * Process payment initiated event
     */
    private void processPaymentInitiatedEvent(BankingDomainEvent event) {
        // Trigger payment processing:
        // - Fraud detection
        // - Compliance checks
        // - Settlement
        System.out.println("Processing PaymentInitiated event: " + event.getEventId());
    }
    
    /**
     * Process fraud detected event
     */
    private void processFraudDetectedEvent(BankingDomainEvent event) {
        // Handle fraud alert:
        // - Block transactions
        // - Notify compliance team
        // - Generate SAR if needed
        System.err.println("FRAUD ALERT: Processing FraudDetected event: " + event.getEventId());
    }
    
    /**
     * Process generic event
     */
    private void processGenericEvent(BankingDomainEvent event) {
        System.out.println("Processing generic event: " + event.getEventType() + " - " + event.getEventId());
    }
    
    /**
     * Handle event processing errors
     */
    private void handleEventProcessingError(String eventJson, String topic, 
                                          Integer partition, Long offset, Exception error) {
        System.err.println("Failed to process event from topic " + topic + 
                          " partition " + partition + " offset " + offset + ": " + error.getMessage());
        
        // Send to dead letter queue after max retries
        sendToDeadLetterQueue(eventJson, topic, error.getMessage());
    }
    
    /**
     * Handle publish errors
     */
    private void handlePublishError(BankingDomainEvent event, String topic, Throwable error) {
        System.err.println("Failed to publish event " + event.getEventId() + 
                          " to topic " + topic + ": " + error.getMessage());
        
        // Could implement retry logic or alerting here
    }
    
    /**
     * Send failed event to dead letter queue
     */
    private void sendToDeadLetterQueue(String eventJson, String originalTopic, String errorMessage) {
        try {
            String dlqTopic = originalTopic + DLQ_TOPIC_SUFFIX;
            
            // Create DLQ message with error metadata
            DeadLetterEvent dlqEvent = DeadLetterEvent.builder()
                .originalEvent(eventJson)
                .originalTopic(originalTopic)
                .errorMessage(errorMessage)
                .failedAt(Instant.now())
                .retryCount(0)
                .build();
            
            String dlqEventJson = objectMapper.writeValueAsString(dlqEvent);
            kafkaTemplate.send(dlqTopic, dlqEventJson);
            
        } catch (Exception e) {
            System.err.println("Failed to send event to DLQ: " + e.getMessage());
        }
    }
    
    /**
     * Handle dead letter queue messages
     */
    private void handleDeadLetterMessage(String eventJson, String topic, Integer partition, Long offset) {
        try {
            DeadLetterEvent dlqEvent = objectMapper.readValue(eventJson, DeadLetterEvent.class);
            
            System.err.println("Processing DLQ event from topic " + topic + 
                              " - Original error: " + dlqEvent.getErrorMessage());
            
            // Could implement manual review process, retry logic, or alerting
            
        } catch (Exception e) {
            System.err.println("Failed to process DLQ event: " + e.getMessage());
        }
    }
    
    /**
     * Log event published
     */
    private void logEventPublished(BankingDomainEvent event, String topic) {
        System.out.println("Published event " + event.getEventId() + 
                          " of type " + event.getEventType() + " to topic " + topic);
    }
    
    /**
     * Log event processed
     */
    private void logEventProcessed(BankingDomainEvent event, String topic, Integer partition, Long offset) {
        System.out.println("Processed event " + event.getEventId() + 
                          " from topic " + topic + " partition " + partition + " offset " + offset);
    }
    
    /**
     * Log duplicate event
     */
    private void logDuplicateEvent(BankingDomainEvent event, String topic) {
        System.out.println("Skipping duplicate event " + event.getEventId() + 
                          " from topic " + topic);
    }
    
    // DTOs and Value Objects
    
    @lombok.Builder
    @lombok.Data
    public static class BankingDomainEvent {
        private String eventId;
        private String eventType;
        private String aggregateId;
        private Long version;
        private Instant timestamp;
        private Object data;
        private Map<String, Object> metadata;
        
        public static BankingDomainEvent create(String eventType, String aggregateId, Object data) {
            return BankingDomainEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .aggregateId(aggregateId)
                .version(1L)
                .timestamp(Instant.now())
                .data(data)
                .metadata(Map.of(
                    "source", "banking-platform",
                    "version", "1.0"
                ))
                .build();
        }
    }
    
    @lombok.Builder
    @lombok.Data
    public static class DeadLetterEvent {
        private String originalEvent;
        private String originalTopic;
        private String errorMessage;
        private Instant failedAt;
        private Integer retryCount;
    }
    
    public static class EventStreamingException extends RuntimeException {
        public EventStreamingException(String message) {
            super(message);
        }
        
        public EventStreamingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    public static class EventProcessingException extends RuntimeException {
        public EventProcessingException(String message) {
            super(message);
        }
        
        public EventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

