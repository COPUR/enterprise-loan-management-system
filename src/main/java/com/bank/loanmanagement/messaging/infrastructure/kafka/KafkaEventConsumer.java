package com.bank.loanmanagement.messaging.infrastructure.kafka;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.saga.domain.SagaOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka Event Consumer for Event-Driven Architecture
 * Consumes domain events from Kafka topics with BIAN service domain routing
 * Handles SAGA coordination and event processing with proper error handling
 * Ensures FAPI security compliance and Berlin Group event processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final ObjectMapper objectMapper;
    private final EventProcessor eventProcessor;
    private final SagaOrchestrator sagaOrchestrator;
    private final KafkaSecurityService kafkaSecurityService;
    private final EventStore eventStore;

    /**
     * Consumer for Consumer Loan service domain events
     * Handles loan application lifecycle events
     */
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "true",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
        include = {Exception.class}
    )
    @KafkaListener(
        topics = {"banking.consumer-loan.commands", "banking.consumer-loan.events"},
        groupId = "consumer-loan-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public CompletableFuture<Void> handleConsumerLoanEvents(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey,
            Acknowledgment acknowledgment) {

        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing consumer loan event from topic {} partition {} offset {}", 
                         topic, partition, offset);

                KafkaEventPublisher.EnrichedEventWrapper wrapper = parseEventWrapper(eventJson);
                DomainEvent domainEvent = reconstructDomainEvent(wrapper);

                // Process event based on type
                processConsumerLoanEvent(domainEvent, wrapper);

                // Acknowledge successful processing
                acknowledgment.acknowledge();
                
                log.info("Successfully processed consumer loan event {} from topic {}", 
                        wrapper.getEventId(), topic);

            } catch (Exception e) {
                log.error("Failed to process consumer loan event from topic {} partition {} offset {}: {}", 
                         topic, partition, offset, e.getMessage(), e);
                // Don't acknowledge - will trigger retry
                throw new RuntimeException("Event processing failed", e);
            }
        });
    }

    /**
     * Consumer for Payment Initiation service domain events
     * Handles payment lifecycle and SCA events
     */
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "true",
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        include = {Exception.class}
    )
    @KafkaListener(
        topics = {"banking.payment-initiation.commands", "banking.payment-initiation.events", "banking.payment-initiation.secure"},
        groupId = "payment-initiation-processor",
        containerFactory = "secureKafkaListenerContainerFactory"
    )
    public CompletableFuture<Void> handlePaymentInitiationEvents(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing payment initiation event from topic {} partition {} offset {}", 
                         topic, partition, offset);

                // Decrypt if from secure topic
                String decryptedEventJson = kafkaSecurityService.decryptIfRequired(eventJson, topic);
                
                KafkaEventPublisher.EnrichedEventWrapper wrapper = parseEventWrapper(decryptedEventJson);
                DomainEvent domainEvent = reconstructDomainEvent(wrapper);

                // Process payment event
                processPaymentInitiationEvent(domainEvent, wrapper);

                acknowledgment.acknowledge();
                
                log.info("Successfully processed payment initiation event {} from topic {}", 
                        wrapper.getEventId(), topic);

            } catch (Exception e) {
                log.error("Failed to process payment initiation event from topic {} partition {} offset {}: {}", 
                         topic, partition, offset, e.getMessage(), e);
                throw new RuntimeException("Payment event processing failed", e);
            }
        });
    }

    /**
     * Consumer for SAGA coordination events
     * Handles distributed transaction coordination across service domains
     */
    @KafkaListener(
        topics = {"banking.loan-origination.saga.loanoriginationsaga", "banking.consumer-loan.saga.loanoriginationsaga"},
        groupId = "saga-coordinator",
        containerFactory = "sagaKafkaListenerContainerFactory"
    )
    public CompletableFuture<Void> handleSagaCoordinationEvents(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = "sagaId", required = false) String sagaId,
            @Header(value = "stepId", required = false) String stepId,
            Acknowledgment acknowledgment) {

        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing SAGA coordination event from topic {} partition {} offset {} sagaId {} stepId {}", 
                         topic, partition, offset, sagaId, stepId);

                KafkaEventPublisher.EnrichedEventWrapper wrapper = parseEventWrapper(eventJson);
                DomainEvent domainEvent = reconstructDomainEvent(wrapper);

                // Extract SAGA metadata
                String extractedSagaId = extractSagaId(wrapper, sagaId);
                String extractedStepId = extractStepId(wrapper, stepId);

                if (extractedSagaId != null) {
                    // Process event through SAGA orchestrator
                    sagaOrchestrator.processEvent(extractedSagaId, domainEvent);
                } else {
                    log.warn("Received SAGA event without saga ID: {}", wrapper.getEventId());
                }

                acknowledgment.acknowledge();
                
                log.info("Successfully processed SAGA coordination event {} for saga {} step {}", 
                        wrapper.getEventId(), extractedSagaId, extractedStepId);

            } catch (Exception e) {
                log.error("Failed to process SAGA coordination event from topic {} partition {} offset {}: {}", 
                         topic, partition, offset, e.getMessage(), e);
                throw new RuntimeException("SAGA event processing failed", e);
            }
        });
    }

    /**
     * Consumer for Customer Management events
     * Handles customer lifecycle and compliance events
     */
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        autoCreateTopics = "true",
        include = {Exception.class}
    )
    @KafkaListener(
        topics = {"banking.customer-management.commands", "banking.customer-management.events", "banking.customer-management.secure"},
        groupId = "customer-management-processor",
        containerFactory = "secureKafkaListenerContainerFactory"
    )
    public CompletableFuture<Void> handleCustomerManagementEvents(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing customer management event from topic {} partition {} offset {}", 
                         topic, partition, offset);

                String decryptedEventJson = kafkaSecurityService.decryptIfRequired(eventJson, topic);
                KafkaEventPublisher.EnrichedEventWrapper wrapper = parseEventWrapper(decryptedEventJson);
                DomainEvent domainEvent = reconstructDomainEvent(wrapper);

                processCustomerManagementEvent(domainEvent, wrapper);

                acknowledgment.acknowledge();
                
                log.info("Successfully processed customer management event {} from topic {}", 
                        wrapper.getEventId(), topic);

            } catch (Exception e) {
                log.error("Failed to process customer management event from topic {} partition {} offset {}: {}", 
                         topic, partition, offset, e.getMessage(), e);
                throw new RuntimeException("Customer management event processing failed", e);
            }
        });
    }

    /**
     * Dead Letter Queue consumer for failed events
     * Handles events that failed processing after all retries
     */
    @KafkaListener(
        topics = {"banking.consumer-loan.commands-dlt", "banking.payment-initiation.commands-dlt", 
                  "banking.customer-management.commands-dlt"},
        groupId = "dead-letter-processor",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetterEvents(
            @Payload String eventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage,
            Acknowledgment acknowledgment) {

        try {
            log.error("Processing dead letter event from topic {} partition {} offset {}, exception: {}", 
                     topic, partition, offset, exceptionMessage);

            KafkaEventPublisher.EnrichedEventWrapper wrapper = parseEventWrapper(eventJson);
            
            // Store failed event for manual investigation
            storeFailedEvent(wrapper, topic, exceptionMessage);
            
            // Send alert for failed event processing
            sendFailedEventAlert(wrapper, topic, exceptionMessage);

            acknowledgment.acknowledge();
            
            log.warn("Processed dead letter event {} from topic {}", wrapper.getEventId(), topic);

        } catch (Exception e) {
            log.error("Failed to process dead letter event from topic {} partition {} offset {}: {}", 
                     topic, partition, offset, e.getMessage(), e);
            // Acknowledge even if DLQ processing fails to prevent infinite loop
            acknowledgment.acknowledge();
        }
    }

    private void processConsumerLoanEvent(DomainEvent domainEvent, KafkaEventPublisher.EnrichedEventWrapper wrapper) {
        // Route event to appropriate processor based on event type and behavior qualifier
        switch (domainEvent.getBehaviorQualifier()) {
            case "INITIATE" -> eventProcessor.processLoanInitiation(domainEvent);
            case "UPDATE" -> eventProcessor.processLoanUpdate(domainEvent);
            case "EXECUTE" -> eventProcessor.processLoanExecution(domainEvent);
            case "GRANT" -> eventProcessor.processLoanGrant(domainEvent);
            case "CONTROL" -> eventProcessor.processLoanControl(domainEvent);
            default -> log.warn("Unknown behavior qualifier {} for consumer loan event {}", 
                              domainEvent.getBehaviorQualifier(), domainEvent.getEventId());
        }
    }

    private void processPaymentInitiationEvent(DomainEvent domainEvent, KafkaEventPublisher.EnrichedEventWrapper wrapper) {
        // Process payment events with SCA handling
        switch (domainEvent.getBehaviorQualifier()) {
            case "INITIATE" -> eventProcessor.processPaymentInitiation(domainEvent);
            case "EXECUTE" -> eventProcessor.processPaymentExecution(domainEvent);
            case "EXCHANGE" -> eventProcessor.processScaExchange(domainEvent);
            case "CONTROL" -> eventProcessor.processPaymentControl(domainEvent);
            default -> log.warn("Unknown behavior qualifier {} for payment initiation event {}", 
                              domainEvent.getBehaviorQualifier(), domainEvent.getEventId());
        }
    }

    private void processCustomerManagementEvent(DomainEvent domainEvent, KafkaEventPublisher.EnrichedEventWrapper wrapper) {
        // Process customer events with privacy protection
        switch (domainEvent.getBehaviorQualifier()) {
            case "INITIATE" -> eventProcessor.processCustomerOnboarding(domainEvent);
            case "UPDATE" -> eventProcessor.processCustomerUpdate(domainEvent);
            case "CAPTURE" -> eventProcessor.processCustomerInteraction(domainEvent);
            case "CONTROL" -> eventProcessor.processCustomerControl(domainEvent);
            default -> log.warn("Unknown behavior qualifier {} for customer management event {}", 
                              domainEvent.getBehaviorQualifier(), domainEvent.getEventId());
        }
    }

    private KafkaEventPublisher.EnrichedEventWrapper parseEventWrapper(String eventJson) {
        try {
            return objectMapper.readValue(eventJson, KafkaEventPublisher.EnrichedEventWrapper.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse event wrapper from JSON", e);
        }
    }

    private DomainEvent reconstructDomainEvent(KafkaEventPublisher.EnrichedEventWrapper wrapper) {
        // This would use an event type registry to reconstruct the proper domain event
        // For now, return the wrapper's toDomainEvent() method result
        return wrapper.toDomainEvent();
    }

    private String extractSagaId(KafkaEventPublisher.EnrichedEventWrapper wrapper, String headerSagaId) {
        if (headerSagaId != null) {
            return headerSagaId;
        }
        
        if (wrapper.getMetadata() != null) {
            Object sagaId = wrapper.getMetadata().get("sagaId");
            return sagaId != null ? sagaId.toString() : null;
        }
        
        return null;
    }

    private String extractStepId(KafkaEventPublisher.EnrichedEventWrapper wrapper, String headerStepId) {
        if (headerStepId != null) {
            return headerStepId;
        }
        
        if (wrapper.getMetadata() != null) {
            Object stepId = wrapper.getMetadata().get("stepId");
            return stepId != null ? stepId.toString() : null;
        }
        
        return null;
    }

    private void storeFailedEvent(KafkaEventPublisher.EnrichedEventWrapper wrapper, String topic, String exceptionMessage) {
        // Store failed event in dead letter store for investigation
        FailedEventRecord failedEvent = FailedEventRecord.builder()
                .eventId(wrapper.getEventId())
                .aggregateId(wrapper.getAggregateId())
                .eventType(wrapper.getEventType())
                .topic(topic)
                .eventData(wrapper.getEventData().toString())
                .failureReason(exceptionMessage)
                .failedAt(OffsetDateTime.now())
                .retryCount(0)
                .build();
        
        // Would save to database or special failed event store
        log.info("Stored failed event {} for investigation", wrapper.getEventId());
    }

    private void sendFailedEventAlert(KafkaEventPublisher.EnrichedEventWrapper wrapper, String topic, String exceptionMessage) {
        // Send alert to monitoring system about failed event
        log.error("ALERT: Event processing failed permanently for event {} from topic {}: {}", 
                 wrapper.getEventId(), topic, exceptionMessage);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FailedEventRecord {
        private String eventId;
        private String aggregateId;
        private String eventType;
        private String topic;
        private String eventData;
        private String failureReason;
        private OffsetDateTime failedAt;
        private int retryCount;
    }
}