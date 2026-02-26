package com.enterprise.openfinance.fxservices.domain.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
