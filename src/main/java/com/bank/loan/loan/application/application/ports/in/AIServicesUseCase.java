package com.bank.loanmanagement.loan.application.ports.in;

import com.bank.loanmanagement.loan.application.commands.*;
import com.bank.loanmanagement.loan.application.results.*;

/**
 * AI Services Use Case (Hexagonal Architecture - Inbound Port)
 * Defines AI-related business operations
 */
public interface AIServicesUseCase {
    
    /**
     * Perform fraud detection analysis
     */
    FraudDetectionResult performFraudDetection(FraudDetectionCommand command);
    
    /**
     * Perform risk assessment
     */
    RiskAssessmentResult performRiskAssessment(RiskAssessmentCommand command);
    
    /**
     * Process RAG query for banking assistance
     */
    RAGQueryResult processRAGQuery(RAGQueryCommand command);
    
    /**
     * Generate AI-powered recommendations
     */
    RecommendationResult generateRecommendations(GenerateRecommendationsCommand command);
}