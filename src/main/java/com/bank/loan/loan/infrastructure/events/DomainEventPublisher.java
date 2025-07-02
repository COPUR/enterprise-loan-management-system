package com.bank.loanmanagement.loan.infrastructure.events;

import com.bank.loanmanagement.loan.domain.shared.AggregateRoot;
import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Infrastructure service for publishing domain events.
 * 
 * Implements Event-Driven Communication by bridging domain events
 * to Spring's application event system for async processing.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Single responsibility for event publishing
 * ✅ Hexagonal: Infrastructure adapter for event publishing
 * ✅ Event-Driven: Enables domain event publishing and handling
 * ✅ DDD: Proper separation of domain and infrastructure concerns
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    private final DomainEventStore eventStore;
    
    /**
     * Publish all uncommitted events from an aggregate root
     */
    @Transactional
    public void publish(AggregateRoot<?> aggregate) {
        if (aggregate == null) {
            log.warn("Attempted to publish events from null aggregate");
            return;
        }
        
        List<DomainEvent> events = aggregate.getUncommittedEvents();
        
        if (events.isEmpty()) {
            log.debug("No uncommitted events to publish for aggregate: {}", aggregate.getAggregateId());
            return;
        }
        
        log.info("Publishing {} domain events for aggregate: {}", events.size(), aggregate.getAggregateId());
        
        for (DomainEvent event : events) {
            try {
                // Store event for event sourcing and audit trail
                eventStore.store(event);
                
                // Publish for immediate async handling
                applicationEventPublisher.publishEvent(event);
                
                log.debug("Published domain event: {} for aggregate: {}", 
                         event.getEventType(), event.getAggregateId());
                         
            } catch (Exception e) {
                log.error("Failed to publish domain event: {} for aggregate: {}", 
                         event.getEventType(), event.getAggregateId(), e);
                throw new DomainEventPublishingException(
                    "Failed to publish domain event: " + event.getEventType(), e);
            }
        }
        
        // Mark events as committed after successful publishing
        aggregate.markEventsAsCommitted();
        
        log.info("Successfully published {} domain events for aggregate: {}", 
                events.size(), aggregate.getAggregateId());
    }
    
    /**
     * Publish a single domain event
     */
    @Transactional
    public void publishSingle(DomainEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null domain event");
            return;
        }
        
        try {
            log.info("Publishing single domain event: {} for aggregate: {}", 
                    event.getEventType(), event.getAggregateId());
            
            // Store event for event sourcing
            eventStore.store(event);
            
            // Publish for immediate handling
            applicationEventPublisher.publishEvent(event);
            
            log.debug("Successfully published domain event: {}", event.getEventType());
            
        } catch (Exception e) {
            log.error("Failed to publish domain event: {}", event.getEventType(), e);
            throw new DomainEventPublishingException(
                "Failed to publish domain event: " + event.getEventType(), e);
        }
    }
    
    /**
     * Business method to get event publishing statistics
     */
    public EventPublishingStats getPublishingStats() {
        return EventPublishingStats.builder()
            .totalEventsStored(eventStore.getTotalEventCount())
            .eventsToday(eventStore.getEventCountForToday())
            .lastEventTime(eventStore.getLastEventTime())
            .build();
    }
    
    /**
     * Business method to check if event publishing is healthy
     */
    public boolean isHealthy() {
        try {
            // Simple health check - try to get stats
            EventPublishingStats stats = getPublishingStats();
            return stats != null;
        } catch (Exception e) {
            log.warn("Event publishing health check failed", e);
            return false;
        }
    }
    
    /**
     * Statistics for event publishing monitoring
     */
    public static class EventPublishingStats {
        private final long totalEventsStored;
        private final long eventsToday;
        private final java.time.LocalDateTime lastEventTime;
        
        private EventPublishingStats(long totalEventsStored, long eventsToday, 
                                   java.time.LocalDateTime lastEventTime) {
            this.totalEventsStored = totalEventsStored;
            this.eventsToday = eventsToday;
            this.lastEventTime = lastEventTime;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public long getTotalEventsStored() { return totalEventsStored; }
        public long getEventsToday() { return eventsToday; }
        public java.time.LocalDateTime getLastEventTime() { return lastEventTime; }
        
        public static class Builder {
            private long totalEventsStored;
            private long eventsToday;
            private java.time.LocalDateTime lastEventTime;
            
            public Builder totalEventsStored(long total) {
                this.totalEventsStored = total;
                return this;
            }
            
            public Builder eventsToday(long today) {
                this.eventsToday = today;
                return this;
            }
            
            public Builder lastEventTime(java.time.LocalDateTime time) {
                this.lastEventTime = time;
                return this;
            }
            
            public EventPublishingStats build() {
                return new EventPublishingStats(totalEventsStored, eventsToday, lastEventTime);
            }
        }
    }
}

/**
 * Exception thrown when domain event publishing fails
 */
class DomainEventPublishingException extends RuntimeException {
    public DomainEventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}