package com.bank.loanmanagement.messaging.infrastructure.security;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import com.bank.loanmanagement.messaging.infrastructure.kafka.KafkaEventPublisher;
import com.bank.loanmanagement.messaging.infrastructure.kafka.KafkaTopicResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Secure Event Publisher with FAPI Integration
 * Publishes events with comprehensive security validation and FAPI compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecureEventPublisher {

    private final KafkaEventPublisher kafkaEventPublisher;
    private final FAPIEventSecurityService fapiEventSecurityService;
    private final KafkaTopicResolver topicResolver;
    private final EventSecurityAuditService auditService;
    
    /**
     * Publish domain event with FAPI security integration
     */
    public CompletableFuture<Void> publishSecureEvent(DomainEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Resolve topic with BIAN compliance
                String topic = topicResolver.resolveTopicForEvent(event);
                
                // Apply FAPI security before publishing
                fapiEventSecurityService.secureEventPublish(event, topic).join();
                
                // Publish to Kafka
                kafkaEventPublisher.publishEvent(event, topic).join();
                
                // Audit successful publication
                auditService.auditEventPublication(event, topic, true, null);
                
                log.debug("Secure event published: {} to topic: {}", event.getEventType(), topic);
                
            } catch (Exception e) {
                log.error("Failed to publish secure event: {}", event.getEventType(), e);
                
                // Audit failed publication
                auditService.auditEventPublication(event, null, false, e.getMessage());
                
                throw new SecureEventPublicationException("Failed to publish secure event", e);
            }
        });
    }
    
    /**
     * Publish batch of events with security validation
     */
    public CompletableFuture<Void> publishSecureEvents(java.util.List<DomainEvent> events) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Process each event with security validation
                for (DomainEvent event : events) {
                    publishSecureEvent(event).join();
                }
                
                log.debug("Published {} secure events successfully", events.size());
                
            } catch (Exception e) {
                log.error("Failed to publish batch of secure events", e);
                throw new SecureEventPublicationException("Failed to publish batch of secure events", e);
            }
        });
    }
    
    /**
     * Publish critical banking event with enhanced security
     */
    public CompletableFuture<Void> publishCriticalBankingEvent(DomainEvent event) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Mark as critical banking event
                event.getMetadata().put("criticalBankingEvent", true);
                event.getMetadata().put("requiresEncryption", true);
                event.getMetadata().put("auditLevel", "HIGH");
                
                // Enhanced security validation for critical events
                validateCriticalEventSecurity(event);
                
                // Publish with standard secure flow
                publishSecureEvent(event).join();
                
                log.info("Critical banking event published: {}", event.getEventType());
                
            } catch (Exception e) {
                log.error("Failed to publish critical banking event: {}", event.getEventType(), e);
                throw new SecureEventPublicationException("Failed to publish critical banking event", e);
            }
        });
    }
    
    /**
     * Validate security for critical banking events
     */
    private void validateCriticalEventSecurity(DomainEvent event) {
        // Ensure critical events have proper metadata
        if (!event.getMetadata().containsKey("customerId")) {
            throw new SecureEventPublicationException("Critical banking events must include customer ID");
        }
        
        // Validate event data completeness
        if (event.getEventData().isEmpty()) {
            throw new SecureEventPublicationException("Critical banking events must have event data");
        }
        
        // Additional critical event validations
        String eventType = event.getEventType();
        if (eventType.contains("Payment") || eventType.contains("Transfer") || eventType.contains("Credit")) {
            validateFinancialEventData(event);
        }
    }
    
    /**
     * Validate financial event data
     */
    private void validateFinancialEventData(DomainEvent event) {
        // Ensure amount is present for financial events
        if (!event.getEventData().containsKey("amount")) {
            throw new SecureEventPublicationException("Financial events must include amount");
        }
        
        // Validate account information
        if (!event.getEventData().containsKey("accountId") && !event.getEventData().containsKey("loanId")) {
            throw new SecureEventPublicationException("Financial events must include account or loan reference");
        }
    }
    
    /**
     * Exception for secure event publication failures
     */
    public static class SecureEventPublicationException extends RuntimeException {
        public SecureEventPublicationException(String message) {
            super(message);
        }
        
        public SecureEventPublicationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}