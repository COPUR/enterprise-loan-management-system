package com.bank.customer.infrastructure.adapter.out.event;

/**
 * Exception thrown when domain event publishing fails.
 */
public class EventPublishingException extends RuntimeException {
    
    public EventPublishingException(String message) {
        super(message);
    }
    
    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}