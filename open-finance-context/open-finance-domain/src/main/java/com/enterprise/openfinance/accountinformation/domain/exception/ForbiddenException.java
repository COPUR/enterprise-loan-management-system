package com.enterprise.openfinance.accountinformation.domain.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
