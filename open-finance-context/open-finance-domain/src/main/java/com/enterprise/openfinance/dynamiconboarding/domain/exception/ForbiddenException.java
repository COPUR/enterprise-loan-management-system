package com.enterprise.openfinance.dynamiconboarding.domain.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
