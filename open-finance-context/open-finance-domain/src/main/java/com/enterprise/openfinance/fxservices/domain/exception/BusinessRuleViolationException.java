package com.enterprise.openfinance.fxservices.domain.exception;

public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
