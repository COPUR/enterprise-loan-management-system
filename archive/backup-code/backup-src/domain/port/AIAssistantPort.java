package com.bank.loanmanagement.domain.port;

import java.util.Map;

/**
 * AI Assistant Port - Domain interface for AI services
 * Follows hexagonal architecture pattern for clean separation of concerns
 */
public interface AIAssistantPort {

    /**
     * Analyze loan application using AI
     */
    Map<String, Object> analyzeLoanApplication(Map<String, Object> applicationData);

    /**
     * Assess credit risk using AI
     */
    Map<String, Object> assessCreditRisk(Map<String, Object> customerData, Map<String, Object> loanData);

    /**
     * Generate loan recommendations
     */
    Map<String, Object> generateLoanRecommendations(Map<String, Object> customerProfile);

    /**
     * Analyze customer financial health
     */
    Map<String, Object> analyzeFinancialHealth(Map<String, Object> financialData);

    /**
     * Detect potential fraud
     */
    Map<String, Object> detectFraud(Map<String, Object> transactionData);

    /**
     * Generate collection strategy
     */
    Map<String, Object> generateCollectionStrategy(Map<String, Object> delinquencyData);

    /**
     * Check AI service health
     */
    Map<String, Object> healthCheck();
}