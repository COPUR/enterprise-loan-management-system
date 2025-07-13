package com.amanahfi.payments.domain.payment;

/**
 * Exception thrown when attempting operations that violate Islamic banking principles
 */
public class IslamicComplianceViolationException extends RuntimeException {
    
    public IslamicComplianceViolationException(String message) {
        super(message);
    }
    
    public IslamicComplianceViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}