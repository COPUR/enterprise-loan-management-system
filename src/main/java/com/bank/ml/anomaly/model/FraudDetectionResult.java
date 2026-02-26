package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Fraud Detection Result
 * Represents the outcome of fraud detection analysis for a transaction
 */
public class FraudDetectionResult {
    
    private final String transactionId;
    private final boolean isFraud;
    private final double riskScore;
    private final double confidenceScore;
    private final String riskLevel;
    private final LocalDateTime analysisTime;
    private final List<String> riskFactors;
    private final Map<String, Object> additionalMetrics;
    
    public FraudDetectionResult(String transactionId, boolean isFraud, double riskScore, 
                               double confidenceScore, String riskLevel, 
                               LocalDateTime analysisTime, List<String> riskFactors,
                               Map<String, Object> additionalMetrics) {
        this.transactionId = transactionId;
        this.isFraud = isFraud;
        this.riskScore = riskScore;
        this.confidenceScore = confidenceScore;
        this.riskLevel = riskLevel;
        this.analysisTime = analysisTime;
        this.riskFactors = riskFactors;
        this.additionalMetrics = additionalMetrics;
    }
    
    // Getters
    public String getTransactionId() { return transactionId; }
    public boolean isFraud() { return isFraud; }
    public double getRiskScore() { return riskScore; }
    public double getConfidenceScore() { return confidenceScore; }
    public String getRiskLevel() { return riskLevel; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public List<String> getRiskFactors() { return riskFactors; }
    public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
}