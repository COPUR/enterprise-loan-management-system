package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Transaction Pattern Analysis Result
 * Contains analysis results for transaction patterns and anomalies
 */
public class TransactionPatternAnalysis {
    
    private final String customerId;
    private final String analysisId;
    private final LocalDateTime analysisTime;
    private final List<String> detectedPatterns;
    private final List<String> anomalies;
    private final double normalityScore;
    private final Map<String, Double> patternMetrics;
    private final String analysisStatus;
    
    public TransactionPatternAnalysis(String customerId, String analysisId, 
                                    LocalDateTime analysisTime, List<String> detectedPatterns,
                                    List<String> anomalies, double normalityScore,
                                    Map<String, Double> patternMetrics, String analysisStatus) {
        this.customerId = customerId;
        this.analysisId = analysisId;
        this.analysisTime = analysisTime;
        this.detectedPatterns = detectedPatterns;
        this.anomalies = anomalies;
        this.normalityScore = normalityScore;
        this.patternMetrics = patternMetrics;
        this.analysisStatus = analysisStatus;
    }
    
    // Getters
    public String getCustomerId() { return customerId; }
    public String getAnalysisId() { return analysisId; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public List<String> getDetectedPatterns() { return detectedPatterns; }
    public List<String> getAnomalies() { return anomalies; }
    public double getNormalityScore() { return normalityScore; }
    public Map<String, Double> getPatternMetrics() { return patternMetrics; }
    public String getAnalysisStatus() { return analysisStatus; }
}