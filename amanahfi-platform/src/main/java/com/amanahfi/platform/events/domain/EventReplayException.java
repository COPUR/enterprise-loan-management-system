package com.amanahfi.platform.events.domain;

/**
 * Exception thrown when event replay operations fail
 */
public class EventReplayException extends RuntimeException {
    
    public EventReplayException(String message) {
        super(message);
    }
    
    public EventReplayException(String message, Throwable cause) {
        super(message, cause);
    }
}