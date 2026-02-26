package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception;

public class InternalTokenUnauthorizedException extends RuntimeException {

    public InternalTokenUnauthorizedException(String message) {
        super(message);
    }
}

