package com.bank.loanmanagement.loan.messaging.infrastructure.kafka;

import com.bank.loan.loan.sharedkernel.domain.event.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based Event Publisher for Event-Driven Architecture
 * Publishes domain events to Kafka topics with BIAN service domain routing
 * Ensures FAPI security compliance and Berlin Group event structure
 * Provides reliable event delivery with retry mechanisms
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicResolver topicResolver;
    private final KafkaSecurityService kafkaSecurityService;

    /**
     * Publish domain event to appropriate Kafka topic
     * Routes events based on BIAN service domain and event type
     */
    public CompletableFuture<SendResult<String, String>> publishEvent(DomainEvent event) {
        return publishEvent(event, Map.of());
    }

    /**
     * Publish domain event with additional metadata
     * Includes FAPI security context and correlation information
     */
    public CompletableFuture<SendResult<String, String>> publishEvent(DomainEvent event, Map<String, Object> metadata) {
        try {
            log.debug("Publishing event {} for aggregate {} to Kafka", 
                     event.getEventType(), event.getAggregateId());

            // Determine topic based on BIAN service domain
            String topic = topicResolver.resolveTopicForEvent(event);
            
            // Create enriched event wrapper
            EnrichedEventWrapper wrapper = createEnrichedWrapper(event, metadata);
            
            // Serialize event
            String eventJson = objectMapper.writeValueAsString(wrapper);
            
            // Apply security if required
            String secureEventJson = kafkaSecurityService.applySecurityIfRequired(eventJson, event);
            
            // Publish to Kafka with partition key based on aggregate ID
            String partitionKey = createPartitionKey(event);
            
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, partitionKey, secureEventJson);
            
            // Add success/failure callbacks
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published event {} to topic {} partition {}", 
                            event.getEventId(), topic, result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish event {} to topic {}: {}", 
                             event.getEventId(), topic, ex.getMessage(), ex);
                }
            });
            
            return future;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {} for Kafka publishing", event.getEventId(), e);
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * Publish batch of events efficiently
     * Maintains order within each aggregate while allowing parallel processing across aggregates
     */
    public CompletableFuture<Void> publishEvents(java.util.List<DomainEvent> events) {
        log.info("Publishing batch of {} events to Kafka", events.size());
        
        java.util.List<CompletableFuture<SendResult<String, String>>> futures = events.stream()
                .map(this::publishEvent)
                .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Successfully published batch of {} events", events.size());
                    } else {
                        log.error("Failed to publish some events in batch", ex);
                    }
                });
    }

    /**
     * Publish SAGA coordination event
     * Special handling for SAGA orchestration events with enhanced metadata
     */
    public CompletableFuture<SendResult<String, String>> publishSagaEvent(DomainEvent event, String sagaId, String stepId) {
        Map<String, Object> sagaMetadata = Map.of(
            "sagaId", sagaId,
            "stepId", stepId,
            "eventCategory", "SAGA_COORDINATION",
            "publishedAt", OffsetDateTime.now().toString()
        );
        
        return publishEvent(event, sagaMetadata);
    }

    private EnrichedEventWrapper createEnrichedWrapper(DomainEvent event, Map<String, Object> metadata) {
        return EnrichedEventWrapper.builder()
                .eventId(event.getEventId())
                .aggregateId(event.getAggregateId())
                .aggregateType(event.getAggregateType())
                .eventType(event.getEventType())
                .eventData(event.getEventData())
                .version(event.getVersion())
                .occurredOn(event.getOccurredOn())
                .serviceDomain(event.getServiceDomain())
                .behaviorQualifier(event.getBehaviorQualifier())
                .metadata(metadata)
                .publishedAt(OffsetDateTime.now())
                .publisher("KafkaEventPublisher")
                .build();
    }

    private String createPartitionKey(DomainEvent event) {
        // Use aggregate ID for partitioning to ensure event ordering per aggregate
        return event.getAggregateId();
    }

    /**
     * Enriched event wrapper for Kafka messages
     * Includes additional metadata for event processing and correlation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EnrichedEventWrapper {
        // Original event data
        private String eventId;
        private String aggregateId;
        private String aggregateType;
        private String eventType;
        private Object eventData;
        private long version;
        private OffsetDateTime occurredOn;
        
        // BIAN compliance data
        private String serviceDomain;
        private String behaviorQualifier;
        
        // Kafka-specific metadata
        private Map<String, Object> metadata;
        private OffsetDateTime publishedAt;
        private String publisher;
        
        // Tracing and correlation
        private String traceId;
        private String correlationId;
        
        // Security context
        private String securityContext;
        private boolean encrypted;
        
        /**
         * Extract original domain event from wrapper
         */
        public DomainEvent toDomainEvent() {
            // This would need to be implemented based on your event type registry
            // For now, return null as placeholder
            return null;
        }
        
        /**
         * Validate wrapper completeness
         */
        public boolean isValid() {
            return eventId != null && 
                   aggregateId != null && 
                   eventType != null && 
                   eventData != null &&
                   serviceDomain != null &&
                   behaviorQualifier != null;
        }
    }
}