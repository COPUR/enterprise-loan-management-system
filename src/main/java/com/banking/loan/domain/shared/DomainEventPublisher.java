package com.banking.loan.domain.shared;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Domain Event Publisher - Infrastructure layer component
 * Publishes domain events to both local and external event systems
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ExternalEventPublisher externalEventPublisher;
    
    /**
     * Publish a single domain event
     */
    public void publish(DomainEvent event) {
        try {
            // Publish locally (for same-service handlers)
            applicationEventPublisher.publishEvent(event);
            log.debug("Published local domain event: {} for aggregate: {}", 
                event.getEventType(), event.getAggregateId());
            
            // Publish externally (for cross-service communication)
            externalEventPublisher.publish(event);
            log.debug("Published external domain event: {} for aggregate: {}", 
                event.getEventType(), event.getAggregateId());
                
        } catch (Exception e) {
            log.error("Failed to publish domain event: {} for aggregate: {}", 
                event.getEventType(), event.getAggregateId(), e);
            throw new EventPublishingException("Failed to publish domain event", e);
        }
    }
    
    /**
     * Publish multiple domain events
     */
    public void publishAll(List<DomainEvent> events) {
        events.forEach(this::publish);
    }
    
    /**
     * Publish events from an aggregate root
     */
    public void publishEventsFrom(AggregateRoot<?> aggregateRoot) {
        List<DomainEvent> events = aggregateRoot.pullDomainEvents();
        if (!events.isEmpty()) {
            publishAll(events);
            log.info("Published {} domain events from aggregate: {}", 
                events.size(), aggregateRoot.getId());
        }
    }
}

