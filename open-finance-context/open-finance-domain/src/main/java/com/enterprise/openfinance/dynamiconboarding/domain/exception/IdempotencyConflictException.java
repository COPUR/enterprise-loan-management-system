package com.enterprise.openfinance.dynamiconboarding.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
