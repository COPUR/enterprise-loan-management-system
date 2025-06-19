package com.bank.loanmanagement.domain.model;

/**
 * Domain Model for User Intent Analysis Results
 * Represents comprehensive analysis results including intent, parameters, and assessment
 */
public class UserIntentAnalysisResult {
    
    private final UserIntentAnalysis intentAnalysis;
    private final FinancialParameters financialParameters;
    private final RequestAssessment requestAssessment;
    private final boolean success;
    private final String error;
    
    private UserIntentAnalysisResult(Builder builder) {
        this.intentAnalysis = builder.intentAnalysis;
        this.financialParameters = builder.financialParameters;
        this.requestAssessment = builder.requestAssessment;
        this.success = builder.success;
        this.error = builder.error;
    }
    
    // Getters
    public UserIntentAnalysis getIntentAnalysis() { return intentAnalysis; }
    public FinancialParameters getFinancialParameters() { return financialParameters; }
    public RequestAssessment getRequestAssessment() { return requestAssessment; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UserIntentAnalysis intentAnalysis;
        private FinancialParameters financialParameters;
        private RequestAssessment requestAssessment;
        private boolean success = false;
        private String error;
        
        public Builder intentAnalysis(UserIntentAnalysis intentAnalysis) {
            this.intentAnalysis = intentAnalysis;
            return this;
        }
        
        public Builder financialParameters(FinancialParameters financialParameters) {
            this.financialParameters = financialParameters;
            return this;
        }
        
        public Builder requestAssessment(RequestAssessment requestAssessment) {
            this.requestAssessment = requestAssessment;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder error(String error) {
            this.error = error;
            return this;
        }
        
        public UserIntentAnalysisResult build() {
            return new UserIntentAnalysisResult(this);
        }
    }
}