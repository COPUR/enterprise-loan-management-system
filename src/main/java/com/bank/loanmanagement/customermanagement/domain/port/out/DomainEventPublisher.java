package com.bank.loanmanagement.customermanagement.domain.port.out;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;

/**
 * Interface for publishing domain events to external systems.
 * Decouples domain from event publishing infrastructure.
 */
public interface DomainEventPublisher {
    
    /**
     * Publish a domain event to external systems (message queues, event bus, etc.).
     */
    void publish(DomainEvent event);
    
    /**
     * Publish multiple domain events in batch.
     */
    void publishAll(Iterable<DomainEvent> events);
}