package com.bank.payment.application;

/**
 * Exception thrown when payment violates compliance rules
 */
public class ComplianceViolationException extends RuntimeException {
    
    public ComplianceViolationException(String message) {
        super(message);
    }
    
    public ComplianceViolationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static ComplianceViolationException forPayment(String paymentId, String violation) {
        return new ComplianceViolationException(
            String.format("Payment %s violates compliance rules: %s", paymentId, violation));
    }
    
    public static ComplianceViolationException generic() {
        return new ComplianceViolationException("Payment violates compliance rules");
    }
}