package com.bank.loanmanagement.ai.application.port.out;

import com.bank.loanmanagement.ai.domain.model.LoanAnalysisRequest;
import com.bank.loanmanagement.ai.domain.model.LoanAnalysisResult;

/**
 * Output port for AI-powered loan analysis
 */
public interface AiLoanAnalysisPort {

    /**
     * Perform AI analysis on loan request
     * 
     * @param request the loan analysis request
     * @return the AI analysis result
     */
    LoanAnalysisResult performAnalysis(LoanAnalysisRequest request);

    /**
     * Check if AI service is available
     * 
     * @return true if AI service is ready
     */
    boolean isAvailable();

    /**
     * Get AI model version being used
     * 
     * @return model version string
     */
    String getModelVersion();
}