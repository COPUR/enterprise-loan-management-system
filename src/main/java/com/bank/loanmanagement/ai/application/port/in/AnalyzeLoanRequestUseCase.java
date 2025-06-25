package com.bank.loanmanagement.ai.application.port.in;

import com.bank.loanmanagement.ai.domain.model.LoanAnalysisResult;

/**
 * Use case port for analyzing loan requests using AI
 */
public interface AnalyzeLoanRequestUseCase {

    /**
     * Analyze a loan request and return AI-powered recommendations
     * 
     * @param command the loan analysis command
     * @return the analysis result with recommendations
     */
    LoanAnalysisResult analyze(AnalyzeLoanRequestCommand command);
}