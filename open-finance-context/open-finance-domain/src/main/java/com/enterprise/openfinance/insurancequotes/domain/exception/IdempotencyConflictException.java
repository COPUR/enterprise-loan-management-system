package com.enterprise.openfinance.insurancequotes.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
