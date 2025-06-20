package com.bank.loanmanagement.application.service;

import com.bank.loanmanagement.application.port.in.BankingAdvisorUseCase;
import com.bank.loanmanagement.application.port.in.SearchKnowledgeBaseQuery;
import com.bank.loanmanagement.application.port.out.AIAnalysisPort;
import com.bank.loanmanagement.application.port.out.KnowledgeBasePort;
import com.bank.loanmanagement.domain.knowledge.KnowledgeQuery;
import com.bank.loanmanagement.domain.knowledge.KnowledgeSearchResult;
import com.bank.loanmanagement.domain.knowledge.KnowledgeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BankingAdvisorService implements BankingAdvisorUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(BankingAdvisorService.class);
    
    private final KnowledgeBasePort knowledgeBasePort;
    private final AIAnalysisPort aiAnalysisPort;
    
    public BankingAdvisorService(KnowledgeBasePort knowledgeBasePort, AIAnalysisPort aiAnalysisPort) {
        this.knowledgeBasePort = knowledgeBasePort;
        this.aiAnalysisPort = aiAnalysisPort;
    }
    
    @Override
    public KnowledgeSearchResult searchKnowledgeBase(SearchKnowledgeBaseQuery query) {
        logger.info("Searching knowledge base for query: {}", query.getQuery());
        
        KnowledgeQuery knowledgeQuery = KnowledgeQuery.builder(query.getQuery())
                .categories(query.getCategories())
                .tags(query.getTags())
                .types(query.getTypes())
                .maxResults(query.getMaxResults())
                .minRelevanceScore(query.getMinRelevanceScore())
                .includeContent(query.isIncludeContent())
                .build();
        
        KnowledgeSearchResult result = knowledgeBasePort.searchKnowledge(knowledgeQuery);
        
        logger.info("Knowledge search completed: {} results found in {}ms", 
                   result.getResultCount(), result.getSearchTimeMs());
        
        return result;
    }
    
    @Override
    public String generateBankingAdvice(String question, String userId) {
        logger.info("Generating banking advice for user: {}", userId);
        
        // Search relevant knowledge
        KnowledgeQuery query = KnowledgeQuery.builder(question)
                .maxResults(5)
                .minRelevanceScore(0.6)
                .build();
        
        KnowledgeSearchResult searchResult = knowledgeBasePort.searchKnowledge(query);
        
        if (!searchResult.hasResults()) {
            logger.warn("No relevant knowledge found for question: {}", question);
            return "I apologize, but I couldn't find relevant information to answer your question. " +
                   "Please contact our customer service team for assistance.";
        }
        
        // Build context from search results
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Based on our banking knowledge base:\n\n");
        
        searchResult.getResults().forEach(knowledge -> {
            contextBuilder.append("- ").append(knowledge.getTitle()).append(": ")
                         .append(knowledge.getContent()).append("\n");
        });
        
        String context = contextBuilder.toString();
        String prompt = String.format(
            "Question: %s\n\nContext: %s\n\nPlease provide a helpful, accurate banking advice based on the context above.",
            question, context
        );
        
        try {
            return aiAnalysisPort.generateAdvice(prompt);
        } catch (Exception e) {
            logger.error("Error generating AI advice", e);
            return "I encountered an error while processing your request. Please try again later.";
        }
    }
    
    @Override
    public String generateLoanRecommendation(String customerId, String loanPurpose) {
        logger.info("Generating loan recommendation for customer: {}, purpose: {}", customerId, loanPurpose);
        
        // Search for loan-related knowledge
        KnowledgeQuery query = KnowledgeQuery.builder(loanPurpose)
                .types(List.of(KnowledgeType.LOAN_CRITERIA, KnowledgeType.PRODUCT_INFO))
                .maxResults(3)
                .minRelevanceScore(0.5)
                .build();
        
        KnowledgeSearchResult searchResult = knowledgeBasePort.searchKnowledge(query);
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Loan products and criteria:\n\n");
        
        searchResult.getResults().forEach(knowledge -> {
            contextBuilder.append("- ").append(knowledge.getTitle()).append(": ")
                         .append(knowledge.getContent()).append("\n");
        });
        
        String context = contextBuilder.toString();
        String prompt = String.format(
            "Customer ID: %s\nLoan Purpose: %s\n\nAvailable Products:\n%s\n\n" +
            "Please recommend the most suitable loan products based on the purpose and available options.",
            customerId, loanPurpose, context
        );
        
        try {
            return aiAnalysisPort.generateAdvice(prompt);
        } catch (Exception e) {
            logger.error("Error generating loan recommendation", e);
            return "Unable to generate loan recommendation at this time. Please contact our loan specialists.";
        }
    }
    
    @Override
    public String generateRiskAssessment(String customerId, String loanType, double amount) {
        logger.info("Generating risk assessment for customer: {}, loan type: {}, amount: {}", 
                   customerId, loanType, amount);
        
        // Search for risk assessment knowledge
        KnowledgeQuery query = KnowledgeQuery.builder(loanType + " risk assessment")
                .types(List.of(KnowledgeType.RISK_ASSESSMENT, KnowledgeType.BUSINESS_RULE))
                .maxResults(3)
                .minRelevanceScore(0.6)
                .build();
        
        KnowledgeSearchResult searchResult = knowledgeBasePort.searchKnowledge(query);
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Risk assessment guidelines:\n\n");
        
        searchResult.getResults().forEach(knowledge -> {
            contextBuilder.append("- ").append(knowledge.getTitle()).append(": ")
                         .append(knowledge.getContent()).append("\n");
        });
        
        String context = contextBuilder.toString();
        String prompt = String.format(
            "Customer ID: %s\nLoan Type: %s\nLoan Amount: %.2f\n\nRisk Guidelines:\n%s\n\n" +
            "Please provide a risk assessment based on the loan type, amount, and guidelines above.",
            customerId, loanType, amount, context
        );
        
        try {
            return aiAnalysisPort.generateAdvice(prompt);
        } catch (Exception e) {
            logger.error("Error generating risk assessment", e);
            return "Unable to complete risk assessment. Please review manually.";
        }
    }
    
    @Override
    public String generateComplianceGuidance(String scenario, String regulation) {
        logger.info("Generating compliance guidance for scenario: {}, regulation: {}", scenario, regulation);
        
        // Search for compliance knowledge
        KnowledgeQuery query = KnowledgeQuery.builder(scenario + " " + regulation)
                .types(List.of(KnowledgeType.COMPLIANCE, KnowledgeType.REGULATION))
                .maxResults(3)
                .minRelevanceScore(0.7)
                .build();
        
        KnowledgeSearchResult searchResult = knowledgeBasePort.searchKnowledge(query);
        
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Compliance requirements:\n\n");
        
        searchResult.getResults().forEach(knowledge -> {
            contextBuilder.append("- ").append(knowledge.getTitle()).append(": ")
                         .append(knowledge.getContent()).append("\n");
        });
        
        String context = contextBuilder.toString();
        String prompt = String.format(
            "Scenario: %s\nRegulation: %s\n\nCompliance Context:\n%s\n\n" +
            "Please provide specific compliance guidance for this scenario based on the regulations above.",
            scenario, regulation, context
        );
        
        try {
            return aiAnalysisPort.generateAdvice(prompt);
        } catch (Exception e) {
            logger.error("Error generating compliance guidance", e);
            return "Unable to generate compliance guidance. Please consult with compliance team.";
        }
    }
    
    @Override
    public boolean isKnowledgeBaseHealthy() {
        return knowledgeBasePort.isHealthy();
    }
    
    @Override
    public long getKnowledgeBaseSize() {
        return knowledgeBasePort.getKnowledgeCount();
    }
}