package com.bank.loanmanagement.application.port.in;

import com.bank.loanmanagement.domain.knowledge.KnowledgeSearchResult;

public interface BankingAdvisorUseCase {
    
    KnowledgeSearchResult searchKnowledgeBase(SearchKnowledgeBaseQuery query);
    
    String generateBankingAdvice(String question, String userId);
    
    String generateLoanRecommendation(String customerId, String loanPurpose);
    
    String generateRiskAssessment(String customerId, String loanType, double amount);
    
    String generateComplianceGuidance(String scenario, String regulation);
    
    boolean isKnowledgeBaseHealthy();
    
    long getKnowledgeBaseSize();
}