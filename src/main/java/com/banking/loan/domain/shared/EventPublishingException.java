package com.banking.loan.domain.shared;

public class EventPublishingException extends RuntimeException {
    public EventPublishingException(String message, Throwable cause) {
        super(message, cause);
    }
}