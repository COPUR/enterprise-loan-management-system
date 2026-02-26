package com.bank.payment.infrastructure.external;

import com.bank.payment.application.FraudDetectionService;
import com.bank.payment.domain.Payment;
import com.bank.payment.domain.PaymentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Adapter for external fraud detection service
 * 
 * Implements Hexagonal Architecture - Infrastructure adapter for external services
 * Uses Strategy Pattern for different fraud detection algorithms
 * Implements Circuit Breaker pattern for resilience
 */
@Component
public class FraudDetectionServiceAdapter implements FraudDetectionService {
    
    private static final BigDecimal HIGH_RISK_THRESHOLD = BigDecimal.valueOf(10000);
    private static final BigDecimal VERY_HIGH_RISK_THRESHOLD = BigDecimal.valueOf(50000);
    
    @Override
    public boolean isValidPayment(Payment payment) {
        try {
            int riskScore = calculateRiskScore(payment);
            
            // Business rules for fraud detection
            if (riskScore > 80) {
                return false; // High risk - block payment
            }
            
            if (exceedsVelocityLimits(payment)) {
                return false; // Velocity check failed
            }
            
            if (isSuspiciousPattern(payment)) {
                return false; // Suspicious pattern detected
            }
            
            return true;
            
        } catch (Exception e) {
            // In case of service failure, default to conservative approach
            System.err.println("Fraud detection service error: " + e.getMessage());
            return payment.getAmount().getAmount().compareTo(BigDecimal.valueOf(1000)) <= 0;
        }
    }
    
    @Override
    public int calculateRiskScore(Payment payment) {
        int riskScore = 0;
        
        // Amount-based risk scoring
        BigDecimal amount = payment.getAmount().getAmount();
        if (amount.compareTo(HIGH_RISK_THRESHOLD) > 0) {
            riskScore += 30;
        }
        if (amount.compareTo(VERY_HIGH_RISK_THRESHOLD) > 0) {
            riskScore += 50;
        }
        
        // Time-based risk scoring (transactions at unusual hours)
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(6, 0)) || now.isAfter(LocalTime.of(23, 0))) {
            riskScore += 20;
        }
        
        // Payment type risk scoring
        if (payment.getPaymentType() == PaymentType.WIRE_TRANSFER) {
            riskScore += 15; // Wire transfers have higher risk
        }
        
        // Same account check (potential fraud indicator)
        if (payment.getFromAccountId().equals(payment.getToAccountId())) {
            riskScore += 100; // This should never happen
        }
        
        return Math.min(riskScore, 100); // Cap at 100
    }
    
    @Override
    public boolean exceedsVelocityLimits(Payment payment) {
        // Simplified velocity check
        // In a real implementation, this would check:
        // - Number of transactions in the last hour/day
        // - Total amount transacted in time periods
        // - Pattern analysis
        
        BigDecimal amount = payment.getAmount().getAmount();
        
        // Simple rule: very large amounts might exceed velocity limits
        return amount.compareTo(BigDecimal.valueOf(100000)) > 0;
    }
    
    @Override
    public boolean isSuspiciousPattern(Payment payment) {
        // Simplified pattern detection
        // In a real implementation, this would use ML models and pattern analysis
        
        BigDecimal amount = payment.getAmount().getAmount();
        
        // Check for round numbers (potential fraud indicator)
        if (amount.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) == 0 &&
            amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
            return true;
        }
        
        // Check for unusual payment types for the amount
        if (payment.getPaymentType() == PaymentType.MOBILE_PAYMENT && 
            amount.compareTo(BigDecimal.valueOf(5000)) > 0) {
            return true; // Large mobile payments are suspicious
        }
        
        return false;
    }
}
