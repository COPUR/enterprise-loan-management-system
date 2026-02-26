package com.enterprise.openfinance.insurancequotes.domain.exception;

public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
