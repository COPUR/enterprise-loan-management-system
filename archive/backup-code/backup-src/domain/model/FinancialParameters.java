package com.bank.loanmanagement.domain.model;

/**
 * Domain Model for Financial Parameters extracted from natural language
 * Represents validated financial information parsed from user input
 */
public class FinancialParameters {
    
    private final double amount;
    private final String timeframe;
    private final String riskTolerance;
    private final String preferredContact;
    private final boolean documentationReady;
    private final String loanPurpose;
    private final double monthlyIncome;
    private final int creditScore;
    private final String employmentStatus;
    
    private FinancialParameters(Builder builder) {
        this.amount = builder.amount;
        this.timeframe = builder.timeframe;
        this.riskTolerance = builder.riskTolerance;
        this.preferredContact = builder.preferredContact;
        this.documentationReady = builder.documentationReady;
        this.loanPurpose = builder.loanPurpose;
        this.monthlyIncome = builder.monthlyIncome;
        this.creditScore = builder.creditScore;
        this.employmentStatus = builder.employmentStatus;
    }
    
    // Getters
    public double getAmount() { return amount; }
    public String getTimeframe() { return timeframe; }
    public String getRiskTolerance() { return riskTolerance; }
    public String getPreferredContact() { return preferredContact; }
    public boolean isDocumentationReady() { return documentationReady; }
    public String getLoanPurpose() { return loanPurpose; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public int getCreditScore() { return creditScore; }
    public String getEmploymentStatus() { return employmentStatus; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private double amount = 0.0;
        private String timeframe = "FLEXIBLE";
        private String riskTolerance = "MODERATE";
        private String preferredContact = "EMAIL";
        private boolean documentationReady = false;
        private String loanPurpose = "GENERAL_PURPOSE";
        private double monthlyIncome = 0.0;
        private int creditScore = 720;
        private String employmentStatus = "EMPLOYED";
        
        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder timeframe(String timeframe) {
            this.timeframe = timeframe;
            return this;
        }
        
        public Builder riskTolerance(String riskTolerance) {
            this.riskTolerance = riskTolerance;
            return this;
        }
        
        public Builder preferredContact(String preferredContact) {
            this.preferredContact = preferredContact;
            return this;
        }
        
        public Builder documentationReady(boolean documentationReady) {
            this.documentationReady = documentationReady;
            return this;
        }
        
        public Builder loanPurpose(String loanPurpose) {
            this.loanPurpose = loanPurpose;
            return this;
        }
        
        public Builder monthlyIncome(double monthlyIncome) {
            this.monthlyIncome = monthlyIncome;
            return this;
        }
        
        public Builder creditScore(int creditScore) {
            this.creditScore = creditScore;
            return this;
        }
        
        public Builder employmentStatus(String employmentStatus) {
            this.employmentStatus = employmentStatus;
            return this;
        }
        
        public FinancialParameters build() {
            return new FinancialParameters(this);
        }
    }
}