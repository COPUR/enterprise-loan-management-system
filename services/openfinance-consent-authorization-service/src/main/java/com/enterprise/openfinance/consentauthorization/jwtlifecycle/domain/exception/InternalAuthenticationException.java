package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.exception;

public class InternalAuthenticationException extends RuntimeException {

    private final boolean throttled;

    public InternalAuthenticationException(String message) {
        this(message, false);
    }

    public InternalAuthenticationException(String message, boolean throttled) {
        super(message);
        this.throttled = throttled;
    }

    public boolean isThrottled() {
        return throttled;
    }
}

