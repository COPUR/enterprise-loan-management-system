package com.amanahfi.payments.domain.payment;

/**
 * Exception thrown when payment amount exceeds configured limits
 */
public class PaymentLimitExceededException extends RuntimeException {
    
    public PaymentLimitExceededException(String message) {
        super(message);
    }
    
    public PaymentLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}