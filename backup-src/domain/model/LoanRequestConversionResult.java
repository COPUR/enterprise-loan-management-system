package com.bank.loanmanagement.domain.model;

/**
 * Domain Model for Loan Request Conversion Results
 * Represents the result of converting natural language to structured loan request
 */
public class LoanRequestConversionResult {
    
    private final String originalPrompt;
    private final LoanRequest convertedRequest;
    private final ValidationResult validationResult;
    private final boolean success;
    private final String error;
    private final double confidence;
    
    private LoanRequestConversionResult(Builder builder) {
        this.originalPrompt = builder.originalPrompt;
        this.convertedRequest = builder.convertedRequest;
        this.validationResult = builder.validationResult;
        this.success = builder.success;
        this.error = builder.error;
        this.confidence = builder.confidence;
    }
    
    // Getters
    public String getOriginalPrompt() { return originalPrompt; }
    public LoanRequest getConvertedRequest() { return convertedRequest; }
    public ValidationResult getValidationResult() { return validationResult; }
    public boolean isSuccess() { return success; }
    public String getError() { return error; }
    public double getConfidence() { return confidence; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String originalPrompt;
        private LoanRequest convertedRequest;
        private ValidationResult validationResult;
        private boolean success = false;
        private String error;
        private double confidence = 0.0;
        
        public Builder originalPrompt(String originalPrompt) {
            this.originalPrompt = originalPrompt;
            return this;
        }
        
        public Builder convertedRequest(LoanRequest convertedRequest) {
            this.convertedRequest = convertedRequest;
            return this;
        }
        
        public Builder validationResult(ValidationResult validationResult) {
            this.validationResult = validationResult;
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
        
        public Builder confidence(double confidence) {
            this.confidence = confidence;
            return this;
        }
        
        public LoanRequestConversionResult build() {
            return new LoanRequestConversionResult(this);
        }
    }
}