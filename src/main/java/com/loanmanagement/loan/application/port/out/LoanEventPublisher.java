package com.loanmanagement.loan.application.port.out;

import com.loanmanagement.shared.domain.DomainEvent;

/**
 * Outbound Port for Publishing Loan Domain Events
 * Abstracts the event publishing mechanism from the application layer
 */
public interface LoanEventPublisher {
    
    /**
     * Publish a single domain event
     */
    void publishEvent(DomainEvent event);
    
    /**
     * Publish multiple domain events
     */
    void publishEvents(java.util.List<DomainEvent> events);
    
    /**
     * Publish event asynchronously
     */
    java.util.concurrent.CompletableFuture<Void> publishEventAsync(DomainEvent event);
}