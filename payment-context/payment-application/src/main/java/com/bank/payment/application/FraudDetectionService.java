package com.bank.payment.application;

import com.bank.payment.domain.Payment;

/**
 * Port interface for fraud detection operations
 * 
 * Follows hexagonal architecture - this is a port that will be implemented
 * by an adapter in the infrastructure layer
 */
public interface FraudDetectionService {
    
    /**
     * Check if payment is valid and not fraudulent
     */
    boolean isValidPayment(Payment payment);
    
    /**
     * Calculate risk score for payment (0-100, higher is riskier)
     */
    int calculateRiskScore(Payment payment);
    
    /**
     * Check if payment exceeds velocity limits
     */
    boolean exceedsVelocityLimits(Payment payment);
    
    /**
     * Check if payment pattern is suspicious
     */
    boolean isSuspiciousPattern(Payment payment);
}