package com.bank.customer.infrastructure.adapter.out.event;

import com.bank.customer.domain.port.out.DomainEventPublisher;
import com.bank.loan.sharedkernel.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Adapter that publishes domain events using Spring's event publishing mechanism.
 * Can be extended to publish to external message queues (Kafka, RabbitMQ, etc.).
 */
@Component
public class DomainEventPublisherAdapter implements DomainEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(DomainEventPublisherAdapter.class);
    
    private final ApplicationEventPublisher springEventPublisher;
    
    public DomainEventPublisherAdapter(ApplicationEventPublisher springEventPublisher) {
        this.springEventPublisher = springEventPublisher;
    }
    
    @Override
    public void publish(DomainEvent event) {
        try {
            logger.debug("Publishing domain event: {}", event);
            
            // Publish to Spring event bus for internal listeners
            springEventPublisher.publishEvent(event);
            
            // TODO: Add external event publishing (Kafka, etc.) when needed
            // kafkaEventPublisher.publish(event);
            
            logger.info("Successfully published domain event: {} with ID: {}", 
                event.getEventType(), event.getEventId());
                
        } catch (Exception e) {
            logger.error("Failed to publish domain event: {} with ID: {}", 
                event.getEventType(), event.getEventId(), e);
            throw new EventPublishingException("Failed to publish domain event: " + event.getEventType(), e);
        }
    }
    
    @Override
    public void publishAll(Iterable<DomainEvent> events) {
        for (DomainEvent event : events) {
            publish(event);
        }
    }
}