package com.banking.loan.domain.shared;

/**
 * External Event Publisher Port (Hexagonal Architecture)
 * For publishing domain events to external systems
 */
public interface ExternalEventPublisher {
    
    /**
     * Publish domain event to external systems
     */
    void publish(DomainEvent event);
    
    /**
     * Publish event with specific routing
     */
    void publishToTopic(DomainEvent event, String topic);
    
    /**
     * Check if publisher is healthy
     */
    boolean isHealthy();
}