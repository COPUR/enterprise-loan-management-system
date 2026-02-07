package com.amanahfi.events.publisher;

import com.amanahfi.events.domain.DomainEvent;
import com.amanahfi.events.domain.EventMetadata;
import com.amanahfi.events.domain.IslamicBankingEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Domain Event Publisher for AmanahFi Islamic Banking Platform
 * 
 * Publishes domain events to Kafka topics with:
 * - Islamic banking compliance headers
 * - UAE regulatory compliance metadata
 * - Audit trail information
 * - Event correlation and tracing
 * - Topic routing based on aggregate type
 */
@Component
public class DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublisher.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventMetadataEnricher metadataEnricher;

    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, 
                               EventMetadataEnricher metadataEnricher) {
        this.kafkaTemplate = kafkaTemplate;
        this.metadataEnricher = metadataEnricher;
    }

    /**
     * Publishes a single domain event
     */
    public CompletableFuture<SendResult<String, Object>> publish(DomainEvent event) {
        try {
            // Enrich event with metadata
            EventMetadata metadata = metadataEnricher.enrich(event);
            
            // Determine topic based on aggregate type
            String topic = determineTopicName(metadata.getAggregateType());
            
            // Create producer record with headers
            ProducerRecord<String, Object> record = createProducerRecord(
                topic, 
                event.getAggregateId(), 
                event, 
                metadata
            );
            
            // Log for audit trail
            auditLogger.info("Publishing event: {} for aggregate: {} to topic: {}", 
                event.getEventType(), event.getAggregateId(), topic);
            
            // Send to Kafka
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(record);
            
            // Log success/failure
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to publish event: {} for aggregate: {}", 
                        event.getEventType(), event.getAggregateId(), throwable);
                    auditLogger.error("Event publishing failed: {} - {}", 
                        metadata.getEventId(), throwable.getMessage());
                } else {
                    logger.debug("Successfully published event: {} for aggregate: {} to partition: {}", 
                        event.getEventType(), event.getAggregateId(), 
                        result.getRecordMetadata().partition());
                    auditLogger.info("Event published successfully: {} - Topic: {} Partition: {} Offset: {}", 
                        metadata.getEventId(), result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
            
            return future;
            
        } catch (Exception e) {
            logger.error("Error publishing event: {} for aggregate: {}", 
                event.getEventType(), event.getAggregateId(), e);
            auditLogger.error("Event publishing error: {} - {}", event.getEventType(), e.getMessage());
            
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    /**
     * Publishes multiple events in batch
     */
    public CompletableFuture<Void> publishBatch(List<DomainEvent> events) {
        logger.info("Publishing batch of {} events", events.size());
        
        List<CompletableFuture<SendResult<String, Object>>> futures = events.stream()
            .map(this::publish)
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Batch publishing failed", throwable);
                    auditLogger.error("Batch event publishing failed: {} events", events.size());
                } else {
                    logger.info("Successfully published batch of {} events", events.size());
                    auditLogger.info("Batch event publishing successful: {} events", events.size());
                }
            });
    }

    /**
     * Creates producer record with Islamic banking compliance headers
     */
    private ProducerRecord<String, Object> createProducerRecord(String topic, String key, 
                                                               DomainEvent event, EventMetadata metadata) {
        Headers headers = new RecordHeaders();
        
        // Basic event headers
        addHeader(headers, "event-id", metadata.getEventId());
        addHeader(headers, "event-type", metadata.getEventType());
        addHeader(headers, "aggregate-type", metadata.getAggregateType());
        addHeader(headers, "aggregate-id", metadata.getAggregateId());
        addHeader(headers, "correlation-id", metadata.getCorrelationId());
        if (metadata.getTimestamp() != null) {
            addHeader(headers, "timestamp", metadata.getTimestamp().toString());
        }
        if (metadata.getVersion() != null) {
            addHeader(headers, "version", metadata.getVersion().toString());
        }
        
        // Islamic Banking Compliance Headers
        if (event instanceof IslamicBankingEvent) {
            addHeader(headers, "islamic-banking", "true");
            addHeader(headers, "sharia-compliant", "true");
            
            if (metadata.isShariahApproved()) {
                addHeader(headers, "sharia-approved", "true");
            }
            
            if (metadata.isCbdcCompliant()) {
                addHeader(headers, "cbdc-compliant", "true");
            }
            
            // Add specific Islamic product headers
            addIslamicProductHeaders(headers, event.getEventType());
        }

        // Regulatory compliance headers apply to all events
        if (metadata.getRegulatoryCompliance() != null) {
            addHeader(headers, "regulatory-compliance", metadata.getRegulatoryCompliance());
        }
        
        // Compliance and Audit Headers
        if (metadata.getComplianceType() != null) {
            addHeader(headers, "compliance-type", metadata.getComplianceType());
        }
        
        if (metadata.isAuditRequired()) {
            addHeader(headers, "audit-required", "true");
            addHeader(headers, "audit-level", metadata.getAuditLevel());
        }
        
        // Settlement time for payment events
        if (event.getEventType().contains("Payment") || event.getEventType().contains("Cbdc")) {
            addPaymentHeaders(headers, event);
        }
        
        return new ProducerRecord<>(topic, null, key, event, headers);
    }

    /**
     * Adds Islamic banking product-specific headers
     */
    private void addIslamicProductHeaders(Headers headers, String eventType) {
        if (eventType.contains("Murabaha")) {
            addHeader(headers, "islamic-product", "MURABAHA");
            addHeader(headers, "asset-backed", "true");
            addHeader(headers, "profit-sharing", "true");
        } else if (eventType.contains("Cbdc")) {
            addHeader(headers, "digital-currency", "UAE-DIRHAM");
            addHeader(headers, "central-bank", "CBUAE");
        } else if (eventType.contains("Account")) {
            addHeader(headers, "islamic-account", "true");
            addHeader(headers, "interest-free", "true");
        } else if (eventType.contains("Payment")) {
            addHeader(headers, "islamic-payment", "true");
            addHeader(headers, "riba-free", "true");
        }
    }

    /**
     * Adds payment-specific headers
     */
    private void addPaymentHeaders(Headers headers, DomainEvent event) {
        // Try to extract settlement time from event if available
        // This is a simplified implementation - in real scenario would use reflection or visitor pattern
        if (event.getClass().getSimpleName().contains("CbdcPaymentSettled")) {
            try {
                // Simplified extraction - in practice would use proper event interfaces
                java.lang.reflect.Method method = event.getClass().getMethod("getSettlementSeconds");
                Object settlementSeconds = method.invoke(event);
                if (settlementSeconds != null) {
                    addHeader(headers, "settlement-time", settlementSeconds.toString());
                }
            } catch (Exception e) {
                logger.debug("Could not extract settlement time from event", e);
            }
        }
    }

    /**
     * Determines Kafka topic name based on aggregate type
     */
    private String determineTopicName(String aggregateType) {
        if (aggregateType == null || aggregateType.isBlank()) {
            return "amanahfi.general";
        }
        return switch (aggregateType.toLowerCase()) {
            case "customer" -> "amanahfi.customers";
            case "account" -> "amanahfi.accounts";
            case "payment" -> "amanahfi.payments";
            case "murabahacontract" -> "amanahfi.murabaha";
            case "compliancecheck" -> "amanahfi.compliance";
            default -> "amanahfi.general";
        };
    }

    /**
     * Helper method to add string header
     */
    private void addHeader(Headers headers, String key, String value) {
        if (value != null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
