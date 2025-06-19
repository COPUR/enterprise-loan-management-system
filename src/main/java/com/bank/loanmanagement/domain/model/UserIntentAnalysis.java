package com.bank.loanmanagement.domain.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain Model for User Intent Analysis Results
 * Represents AI-powered analysis of user intentions and banking service needs
 */
public class UserIntentAnalysis {
    
    private final String originalInput;
    private final String primaryIntent;
    private final List<String> secondaryIntents;
    private final String urgencyLevel;
    private final String customerSentiment;
    private final String complexityLevel;
    private final FinancialParameters extractedParameters;
    private final List<String> recommendedWorkflow;
    private final String estimatedProcessingTime;
    private final double confidence;
    private final LocalDateTime timestamp;
    
    private UserIntentAnalysis(Builder builder) {
        this.originalInput = builder.originalInput;
        this.primaryIntent = builder.primaryIntent;
        this.secondaryIntents = builder.secondaryIntents;
        this.urgencyLevel = builder.urgencyLevel;
        this.customerSentiment = builder.customerSentiment;
        this.complexityLevel = builder.complexityLevel;
        this.extractedParameters = builder.extractedParameters;
        this.recommendedWorkflow = builder.recommendedWorkflow;
        this.estimatedProcessingTime = builder.estimatedProcessingTime;
        this.confidence = builder.confidence;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
    }
    
    // Getters
    public String getOriginalInput() { return originalInput; }
    public String getPrimaryIntent() { return primaryIntent; }
    public List<String> getSecondaryIntents() { return secondaryIntents; }
    public String getUrgencyLevel() { return urgencyLevel; }
    public String getCustomerSentiment() { return customerSentiment; }
    public String getComplexityLevel() { return complexityLevel; }
    public FinancialParameters getExtractedParameters() { return extractedParameters; }
    public List<String> getRecommendedWorkflow() { return recommendedWorkflow; }
    public String getEstimatedProcessingTime() { return estimatedProcessingTime; }
    public double getConfidence() { return confidence; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String originalInput;
        private String primaryIntent;
        private List<String> secondaryIntents;
        private String urgencyLevel;
        private String customerSentiment;
        private String complexityLevel;
        private FinancialParameters extractedParameters;
        private List<String> recommendedWorkflow;
        private String estimatedProcessingTime;
        private double confidence;
        private LocalDateTime timestamp;
        
        public Builder originalInput(String originalInput) {
            this.originalInput = originalInput;
            return this;
        }
        
        public Builder primaryIntent(String primaryIntent) {
            this.primaryIntent = primaryIntent;
            return this;
        }
        
        public Builder secondaryIntents(List<String> secondaryIntents) {
            this.secondaryIntents = secondaryIntents;
            return this;
        }
        
        public Builder urgencyLevel(String urgencyLevel) {
            this.urgencyLevel = urgencyLevel;
            return this;
        }
        
        public Builder customerSentiment(String customerSentiment) {
            this.customerSentiment = customerSentiment;
            return this;
        }
        
        public Builder complexityLevel(String complexityLevel) {
            this.complexityLevel = complexityLevel;
            return this;
        }
        
        public Builder extractedParameters(FinancialParameters extractedParameters) {
            this.extractedParameters = extractedParameters;
            return this;
        }
        
        public Builder recommendedWorkflow(List<String> recommendedWorkflow) {
            this.recommendedWorkflow = recommendedWorkflow;
            return this;
        }
        
        public Builder estimatedProcessingTime(String estimatedProcessingTime) {
            this.estimatedProcessingTime = estimatedProcessingTime;
            return this;
        }
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public UserIntentAnalysis build() {
            return new UserIntentAnalysis(this);
        }
    }
}