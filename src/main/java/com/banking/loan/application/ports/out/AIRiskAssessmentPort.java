package com.banking.loan.application.ports.out;

import com.banking.loan.domain.loan.AIRiskAssessment;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Outbound Port for AI Risk Assessment (Hexagonal Architecture)
 * External service integration contract
 */
public interface AIRiskAssessmentPort {
    
    /**
     * Perform AI-based risk assessment for loan application
     */
    AIRiskAssessment assessLoanRisk(
        String customerId,
        BigDecimal loanAmount,
        Integer termInMonths,
        Map<String, Object> customerData
    );
    
    /**
     * Get risk assessment by ID
     */
    AIRiskAssessment getRiskAssessment(String assessmentId);
    
    /**
     * Check if AI service is available
     */
    boolean isServiceAvailable();
    
    /**
     * Initiate asynchronous risk assessment
     */
    void initiateAssessment(String loanId, String correlationId);
}