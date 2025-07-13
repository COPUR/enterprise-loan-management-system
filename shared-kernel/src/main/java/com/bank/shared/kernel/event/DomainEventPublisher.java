package com.bank.shared.kernel.event;

import com.bank.shared.kernel.domain.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Domain Event Publisher - Infrastructure concern in shared kernel
 * 
 * Implements Event-Driven Architecture (EDA) by publishing domain events
 * to enable loose coupling between bounded contexts
 */
@Component
public class DomainEventPublisher {
    
    private final ApplicationEventPublisher applicationEventPublisher;
    
    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }
    
    /**
     * Publish a domain event to the event bus
     */
    public void publish(DomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
    
    /**
     * Publish multiple domain events
     */
    public void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }
}