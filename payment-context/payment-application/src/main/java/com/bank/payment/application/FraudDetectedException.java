package com.bank.payment.application;

/**
 * Exception thrown when payment is flagged by fraud detection
 */
public class FraudDetectedException extends RuntimeException {
    
    public FraudDetectedException(String message) {
        super(message);
    }
    
    public FraudDetectedException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static FraudDetectedException forPayment(String paymentId, String reason) {
        return new FraudDetectedException(
            String.format("Payment %s flagged for fraud: %s", paymentId, reason));
    }
    
    public static FraudDetectedException generic() {
        return new FraudDetectedException("Payment flagged by fraud detection system");
    }
}