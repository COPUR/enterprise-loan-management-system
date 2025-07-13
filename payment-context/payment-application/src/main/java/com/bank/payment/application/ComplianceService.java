package com.bank.payment.application;

import com.bank.payment.domain.Payment;

/**
 * Port interface for compliance validation operations
 * 
 * Follows hexagonal architecture - this is a port that will be implemented
 * by an adapter in the infrastructure layer
 */
public interface ComplianceService {
    
    /**
     * Validate payment against compliance rules
     */
    boolean validatePayment(Payment payment);
    
    /**
     * Check if payment requires KYC verification
     */
    boolean requiresKycVerification(Payment payment);
    
    /**
     * Check if payment exceeds reporting thresholds
     */
    boolean exceedsReportingThreshold(Payment payment);
    
    /**
     * Check if payment involves sanctioned entities
     */
    boolean involvesSanctionedEntities(Payment payment);
    
    /**
     * Generate compliance report for payment
     */
    String generateComplianceReport(Payment payment);
}