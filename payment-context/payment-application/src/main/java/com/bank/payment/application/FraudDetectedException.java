package com.bank.payment.application;

import java.util.List;
import java.util.Collections;

/**
 * Exception thrown when payment is flagged by fraud detection
 * 
 * Enhanced to support ML-based fraud detection with risk scores and detailed risk factors
 */
public class FraudDetectedException extends RuntimeException {
    
    private final int riskScore;
    private final List<String> riskFactors;
    
    public FraudDetectedException(String message) {
        super(message);
        this.riskScore = 100;
        this.riskFactors = Collections.emptyList();
    }
    
    public FraudDetectedException(String message, Throwable cause) {
        super(message, cause);
        this.riskScore = 100;
        this.riskFactors = Collections.emptyList();
    }
    
    public FraudDetectedException(String message, int riskScore, List<String> riskFactors) {
        super(message);
        this.riskScore = riskScore;
        this.riskFactors = riskFactors != null ? List.copyOf(riskFactors) : Collections.emptyList();
    }
    
    public static FraudDetectedException forPayment(String paymentId, String reason) {
        return new FraudDetectedException(
            String.format("Payment %s flagged for fraud: %s", paymentId, reason));
    }
    
    public static FraudDetectedException forPayment(String paymentId, String reason, int riskScore, List<String> riskFactors) {
        return new FraudDetectedException(
            String.format("Payment %s flagged for fraud: %s", paymentId, reason),
            riskScore,
            riskFactors
        );
    }
    
    public static FraudDetectedException generic() {
        return new FraudDetectedException("Payment flagged by fraud detection system");
    }
    
    public int getRiskScore() {
        return riskScore;
    }
    
    public List<String> getRiskFactors() {
        return riskFactors;
    }
}