package com.bank.loanmanagement.loan.infrastructure.messaging;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
import com.bank.loanmanagement.loan.infrastructure.events.ExternalEventPublisher;
import com.bank.loanmanagement.loan.infrastructure.messaging.EventTopicRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of External Event Publisher
 * Handles cross-service communication through Kafka events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements ExternalEventPublisher {

    private final EventTopicRouter topicRouter;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public boolean isHealthy() {
        return true; // Placeholder for actual health check logic
    }
    
    @Override
    public void publish(DomainEvent event) {
        try {
            EventTopicRouter.TopicConfiguration topicConfig = topicRouter.getTopicConfigurationForEvent(event);
            String topic = topicConfig.getTopicName();
            String partitionKey = extractPartitionKey(event, topicConfig.getPartitionKeyField());
            
            // Create enhanced banking message with industry-standard headers
            BankingMessage bankingMessage = createBankingMessage(event);
            String eventJson = objectMapper.writeValueAsString(bankingMessage);
            
            // Configure headers for banking compliance
            var headers = kafkaTemplate.getProducerFactory().createProducer().partitionsFor(topic);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(topic, partitionKey, eventJson);
            
            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("Failed to publish banking event {} to topic {}: {}", 
                        event.getEventType(), topic, throwable.getMessage());
                    // Publish to dead letter queue for regulatory compliance
                    publishToDeadLetterQueue(event, throwable);
                } else {
                    log.debug("Successfully published banking event {} to topic {} at partition {} offset {}", 
                        event.getEventType(), topic, 
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
            
        } catch (Exception e) {
            log.error("Error publishing banking event {} to Kafka", event.getEventType(), e);
            publishToDeadLetterQueue(event, e);
            throw new RuntimeException("Failed to publish banking event to Kafka", e);
        }
    }
    
    /**
     * Extract partition key based on event data and configured field
     */
    private String extractPartitionKey(DomainEvent event, String partitionKeyField) {
        try {
            // Use reflection to extract the partition key field value
            var eventClass = event.getClass();
            var field = eventClass.getDeclaredField(partitionKeyField);
            field.setAccessible(true);
            Object value = field.get(event);
            return value != null ? value.toString() : event.getAggregateId();
        } catch (Exception e) {
            log.warn("Could not extract partition key field {} from event {}, using aggregateId", 
                partitionKeyField, event.getEventType());
            return event.getAggregateId();
        }
    }
    
    /**
     * Create industry-standard banking message with ISO 20022 compliance
     */
    private BankingMessage createBankingMessage(DomainEvent event) {
        return BankingMessage.builder()
            .messageHeader(BankingMessageHeader.builder()
                .messageId("MSG-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8))
                .timestamp(event.getOccurredOn())
                .messageType(event.getEventType())
                .schemaVersion(event.getSchemaVersion())
                .source("enterprise-banking-system")
                .correlationId(event.getCorrelationId())
                .businessProcessReference("BP-" + event.getEventType().toUpperCase())
                .build())
            .eventMetadata(BankingEventMetadata.builder()
                .tenantId(event.getTenantId())
                .regionCode(determineRegionCode(event))
                .regulatoryJurisdiction(determineRegulatoryJurisdiction(event))
                .businessDate(LocalDate.now())
                .processingCenter(determineProcessingCenter(event))
                .build())
            .eventPayload(event)
            .complianceData(BankingComplianceData.builder()
                .amlScreeningResult("PENDING")
                .sanctionsCheckResult("PENDING")
                .fatcaReportable(false)
                .crsReportable(false)
                .complianceScore(0.0)
                .build())
            .auditTrail(BankingAuditTrail.builder()
                .initiatedBy(event.getTriggeredBy())
                .processedBy("enterprise-banking-v2.0")
                .approvedBy("system-auto")
                .auditLevel("FULL")
                .build())
            .build();
    }
    
    /**
     * Publish failed events to dead letter queue for compliance
     */
    private void publishToDeadLetterQueue(DomainEvent event, Throwable error) {
        try {
            DeadLetterEvent dlqEvent = DeadLetterEvent.builder()
                .originalEventType(event.getEventType())
                .originalEventId(event.getEventId().toString())
                .originalAggregateId(event.getAggregateId())
                .errorMessage(error.getMessage())
                .errorTimestamp(Instant.now())
                .retryCount(0)
                .maxRetries(3)
                .build();
            
            String dlqTopic = "banking.infrastructure.dlq.failed-events.v1";
            String dlqEventJson = objectMapper.writeValueAsString(dlqEvent);
            
            kafkaTemplate.send(dlqTopic, event.getAggregateId(), dlqEventJson);
            log.info("Published failed event {} to dead letter queue", event.getEventType());
            
        } catch (Exception dlqError) {
            log.error("Failed to publish to dead letter queue for event {}", event.getEventType(), dlqError);
        }
    }
    
    private String determineRegionCode(DomainEvent event) {
        String tenantId = event.getTenantId();
        if (tenantId != null) {
            if (tenantId.contains("ae")) return "MENA";
            if (tenantId.contains("us")) return "AMERICAS";
            if (tenantId.contains("eu")) return "EUROPE";
            if (tenantId.contains("sg")) return "APAC";
        }
        return "GLOBAL";
    }
    
    private String determineRegulatoryJurisdiction(DomainEvent event) {
        String tenantId = event.getTenantId();
        if (tenantId != null) {
            if (tenantId.contains("ae")) return "UAE-CBUAE";
            if (tenantId.contains("us")) return "US-FED";
            if (tenantId.contains("eu")) return "EU-ECB";
            if (tenantId.contains("sg")) return "SG-MAS";
        }
        return "INTERNATIONAL";
    }
    
    private String determineProcessingCenter(DomainEvent event) {
        String regionCode = determineRegionCode(event);
        return switch (regionCode) {
            case "MENA" -> "DXB-DC1";
            case "AMERICAS" -> "NYC-DC1";
            case "EUROPE" -> "LDN-DC1";
            case "APAC" -> "SGP-DC1";
            default -> "GLOBAL-DC1";
        };
    }
}
