package com.bank.loanmanagement.ai.application.port.out;

import com.bank.loanmanagement.ai.domain.model.LoanAnalysisRequest;

/**
 * Output port for AI-powered fraud detection
 */
public interface FraudDetectionPort {

    /**
     * Analyze loan request for fraud indicators
     * 
     * @param request the loan analysis request
     * @return fraud analysis result with risk indicators
     */
    FraudAnalysisResult analyzeFraudRisk(LoanAnalysisRequest request);

    /**
     * Check if fraud detection service is available
     * 
     * @return true if fraud detection service is ready
     */
    boolean isAvailable();

    /**
     * Fraud analysis result
     */
    record FraudAnalysisResult(
        boolean hasFraudRisk,
        double fraudScore,
        String riskIndicators,
        String recommendation
    ) {}
}