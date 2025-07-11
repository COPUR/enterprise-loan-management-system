package com.amanahfi.platform.shared.events;

import com.amanahfi.platform.shared.domain.DomainEvent;

import java.util.List;

/**
 * Port for publishing domain events in the AmanahFi Platform
 * 
 * Single responsibility: Publishing domain events to the event infrastructure.
 * This interface follows the Single Responsibility Principle by focusing
 * solely on event publishing without mixing concerns like routing or topics.
 * 
 * Design Principles:
 * - Single Responsibility: Only publishes events
 * - Hexagonal Architecture: Output port from domain
 * - Dependency Inversion: Domain depends on abstraction
 * - Interface Segregation: Minimal, focused interface
 * 
 * @author AmanahFi Development Team
 * @version 1.0.0
 * @since 2024
 */
public interface DomainEventPublisher {

    /**
     * Publishes a single domain event
     * 
     * @param event The domain event to publish
     * @throws EventPublishingException if publishing fails
     */
    void publish(DomainEvent event);

    /**
     * Publishes multiple domain events atomically
     * 
     * @param events The list of domain events to publish
     * @throws EventPublishingException if publishing fails
     */
    void publishAll(List<DomainEvent> events);

    /**
     * Exception thrown when event publishing fails
     */
    class EventPublishingException extends RuntimeException {
        
        public EventPublishingException(String message) {
            super(message);
        }
        
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}