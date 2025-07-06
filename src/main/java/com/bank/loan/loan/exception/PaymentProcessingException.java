package com.bank.loan.loan.exception;

/**
 * Exception thrown when payment processing fails
 */
public class PaymentProcessingException extends RuntimeException {
    
    private final String errorCode;
    
    public PaymentProcessingException(String message) {
        super(message);
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
    }
    
    public PaymentProcessingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PAYMENT_PROCESSING_ERROR";
    }
    
    public PaymentProcessingException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}