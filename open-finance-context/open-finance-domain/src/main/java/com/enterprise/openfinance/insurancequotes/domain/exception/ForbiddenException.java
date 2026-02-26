package com.enterprise.openfinance.insurancequotes.domain.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
