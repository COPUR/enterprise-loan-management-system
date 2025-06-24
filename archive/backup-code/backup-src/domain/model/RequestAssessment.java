package com.bank.loanmanagement.domain.model;

/**
 * Domain Model for Request Assessment
 * Represents urgency and complexity analysis of user banking requests
 */
public class RequestAssessment {
    
    private final String urgencyLevel;
    private final String complexityLevel;
    private final String priorityScore;
    private final String estimatedResolution;
    private final boolean requiresSpecialistReview;
    private final String recommendedChannel;
    
    private RequestAssessment(Builder builder) {
        this.urgencyLevel = builder.urgencyLevel;
        this.complexityLevel = builder.complexityLevel;
        this.priorityScore = builder.priorityScore;
        this.estimatedResolution = builder.estimatedResolution;
        this.requiresSpecialistReview = builder.requiresSpecialistReview;
        this.recommendedChannel = builder.recommendedChannel;
    }
    
    // Getters
    public String getUrgencyLevel() { return urgencyLevel; }
    public String getComplexityLevel() { return complexityLevel; }
    public String getPriorityScore() { return priorityScore; }
    public String getEstimatedResolution() { return estimatedResolution; }
    public boolean requiresSpecialistReview() { return requiresSpecialistReview; }
    public String getRecommendedChannel() { return recommendedChannel; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String urgencyLevel = "LOW";
        private String complexityLevel = "LOW";
        private String priorityScore = "STANDARD";
        private String estimatedResolution = "1-2 business days";
        private boolean requiresSpecialistReview = false;
        private String recommendedChannel = "DIGITAL";
        
        public Builder urgencyLevel(String urgencyLevel) {
            this.urgencyLevel = urgencyLevel;
            return this;
        }
        
        public Builder complexityLevel(String complexityLevel) {
            this.complexityLevel = complexityLevel;
            return this;
        }
        
        public Builder priorityScore(String priorityScore) {
            this.priorityScore = priorityScore;
            return this;
        }
        
        public Builder estimatedResolution(String estimatedResolution) {
            this.estimatedResolution = estimatedResolution;
            return this;
        }
        
        public Builder requiresSpecialistReview(boolean requiresSpecialistReview) {
            this.requiresSpecialistReview = requiresSpecialistReview;
            return this;
        }
        
        public Builder recommendedChannel(String recommendedChannel) {
            this.recommendedChannel = recommendedChannel;
            return this;
        }
        
        public RequestAssessment build() {
            return new RequestAssessment(this);
        }
    }
}