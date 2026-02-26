package com.enterprise.openfinance.dynamiconboarding.domain.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
