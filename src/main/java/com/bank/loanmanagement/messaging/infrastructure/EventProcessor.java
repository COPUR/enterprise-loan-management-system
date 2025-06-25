package com.bank.loanmanagement.messaging.infrastructure;

import com.bank.loanmanagement.sharedkernel.domain.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Basic implementation of Event Processor
 * Handles domain event processing with BIAN compliance
 */
@Service
@Slf4j
public class EventProcessor {
    
    /**
     * Process a domain event
     */
    public CompletableFuture<Void> processEvent(DomainEvent event, String topic) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing event {} from topic {}", event.getEventType(), topic);
                // Basic implementation - log the event
                log.info("Processed {} event for aggregate {}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Error processing event {} from topic {}", event.getEventType(), topic, e);
                throw new EventProcessingException("Failed to process event", e);
            }
        });
    }
    
    /**
     * Process a SAGA event
     */
    public CompletableFuture<Void> processSagaEvent(DomainEvent event, String topic, String sagaId) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing SAGA event {} for SAGA {} from topic {}", event.getEventType(), sagaId, topic);
                // Basic implementation - log the event
                log.info("Processed SAGA {} event for aggregate {}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Error processing SAGA event {} for SAGA {} from topic {}", event.getEventType(), sagaId, topic, e);
                throw new EventProcessingException("Failed to process SAGA event", e);
            }
        });
    }
    
    /**
     * Process a secure event
     */
    public CompletableFuture<Void> processSecureEvent(DomainEvent event, String topic) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Processing secure event {} from topic {}", event.getEventType(), topic);
                // Basic implementation - log the event
                log.info("Processed secure {} event for aggregate {}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Error processing secure event {} from topic {}", event.getEventType(), topic, e);
                throw new EventProcessingException("Failed to process secure event", e);
            }
        });
    }
    
    /**
     * Event processing exception
     */
    public static class EventProcessingException extends RuntimeException {
        public EventProcessingException(String message) {
            super(message);
        }
        
        public EventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}