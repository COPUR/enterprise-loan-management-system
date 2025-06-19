package com.bank.loanmanagement.domain.model;

/**
 * Domain Model for Banking Request Processing Results
 * Represents comprehensive end-to-end processing results for natural language banking requests
 */
public class BankingRequestProcessingResult {
    
    private final String userInput;
    private final UserIntentAnalysis intentAnalysis;
    private final RequestAssessment requestAssessment;
    private final LoanRequestConversionResult loanConversion;
    private final String recommendedAction;
    private final boolean success;
    private final String error;
    
    private BankingRequestProcessingResult(Builder builder) {
        this.userInput = builder.userInput;
        this.intentAnalysis = builder.intentAnalysis;
        this.requestAssessment = builder.requestAssessment;
        this.loanConversion = builder.loanConversion;
        this.recommendedAction = builder.recommendedAction;
        this.success = builder.success;
        this.error = builder.error;
    }
    
    // Getters
    public String getUserInput() { return userInput; }
    public UserIntentAnalysis getIntentAnalysis() { return intentAnalysis; }
    public RequestAssessment getRequestAssessment() { return requestAssessment; }
    public LoanRequestConversionResult getLoanConversion() { return loanConversion; }
    public String getRecommendedAction() { return recommendedAction; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    
    public BankingRequestProcessingResult withLoanConversion(LoanRequestConversionResult loanConversion) {
        return builder()
                .userInput(this.userInput)
                .intentAnalysis(this.intentAnalysis)
                .requestAssessment(this.requestAssessment)
                .loanConversion(loanConversion)
                .recommendedAction(this.recommendedAction)
                .success(this.success)
                .error(this.error)
                .build();
    }
    
    public BankingRequestProcessingResult withRecommendedAction(String recommendedAction) {
        return builder()
                .userInput(this.userInput)
                .intentAnalysis(this.intentAnalysis)
                .requestAssessment(this.requestAssessment)
                .loanConversion(this.loanConversion)
                .recommendedAction(recommendedAction)
                .success(this.success)
                .error(this.error)
                .build();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String userInput;
        private UserIntentAnalysis intentAnalysis;
        private RequestAssessment requestAssessment;
        private LoanRequestConversionResult loanConversion;
        private String recommendedAction;
        private boolean success = false;
        private String error;
        
        public Builder userInput(String userInput) {
            this.userInput = userInput;
            return this;
        }
        
        public Builder intentAnalysis(UserIntentAnalysis intentAnalysis) {
            this.intentAnalysis = intentAnalysis;
            return this;
        }
        
        public Builder requestAssessment(RequestAssessment requestAssessment) {
            this.requestAssessment = requestAssessment;
            return this;
        }
        
        public Builder loanConversion(LoanRequestConversionResult loanConversion) {
            this.loanConversion = loanConversion;
            return this;
        }
        
        public Builder recommendedAction(String recommendedAction) {
            this.recommendedAction = recommendedAction;
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
        
        public BankingRequestProcessingResult build() {
            return new BankingRequestProcessingResult(this);
        }
    }
}