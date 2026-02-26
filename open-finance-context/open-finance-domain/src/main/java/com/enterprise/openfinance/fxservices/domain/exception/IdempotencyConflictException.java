package com.enterprise.openfinance.fxservices.domain.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String message) {
        super(message);
    }
}
