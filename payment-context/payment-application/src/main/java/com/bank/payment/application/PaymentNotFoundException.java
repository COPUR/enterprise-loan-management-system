package com.bank.payment.application;

/**
 * Exception thrown when a payment cannot be found
 */
public class PaymentNotFoundException extends RuntimeException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
    
    public PaymentNotFoundException(String paymentId, Throwable cause) {
        super("Payment not found with ID: " + paymentId, cause);
    }
    
    public static PaymentNotFoundException withId(String paymentId) {
        return new PaymentNotFoundException("Payment not found with ID: " + paymentId);
    }
}