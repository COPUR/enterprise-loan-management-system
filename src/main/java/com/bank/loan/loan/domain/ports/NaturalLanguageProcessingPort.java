package com.bank.loanmanagement.loan.domain.port;

import com.bank.loanmanagement.loan.domain.model.LoanRequest;
import com.bank.loanmanagement.loan.domain.model.UserIntentAnalysis;

/**
 * Domain Port for Natural Language Processing Services
 * Provides AI-powered conversion of user prompts to structured banking requests
 */
public interface NaturalLanguageProcessingPort {
    
    /**
     * Convert natural language prompt to structured loan request
     * @param userPrompt Natural language description of loan needs
     * @return Structured loan request with extracted parameters
     */
    LoanRequest convertPromptToLoanRequest(String userPrompt);
    
    /**
     * Analyze user intent and classify banking service request
     * @param userInput Natural language user input
     * @return Detailed intent analysis with recommended actions
     */
    UserIntentAnalysis analyzeUserIntent(String userInput);
    
    /**
     * Extract and validate financial parameters from text
     * @param text Natural language text containing financial information
     * @return Validated financial parameters
     */
    FinancialParameters extractFinancialParameters(String text);
    
    /**
     * Assess urgency and complexity of user request
     * @param userInput Natural language user input
     * @return Request priority and complexity assessment
     */
    RequestAssessment assessRequestComplexity(String userInput);
}