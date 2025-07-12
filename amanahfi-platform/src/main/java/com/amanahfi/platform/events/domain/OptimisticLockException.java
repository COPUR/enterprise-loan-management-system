package com.amanahfi.platform.events.domain;

/**
 * Exception thrown when optimistic locking fails in event store
 */
public class OptimisticLockException extends RuntimeException {
    
    public OptimisticLockException(String message) {
        super(message);
    }
    
    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}