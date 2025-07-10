package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Alert Correlation Result
 * Represents the result of correlating alerts across regions
 */
public class AlertCorrelationResult {
    
    private final String correlationId;
    private final LocalDateTime analysisTime;
    private final List<String> affectedRegions;
    private final String alertType;
    private final double correlationScore;
    private final Map<String, Object> correlationMetrics;
    
    public AlertCorrelationResult(String correlationId, LocalDateTime analysisTime,
                                 List<String> affectedRegions, String alertType,
                                 double correlationScore, Map<String, Object> correlationMetrics) {
        this.correlationId = correlationId;
        this.analysisTime = analysisTime;
        this.affectedRegions = affectedRegions;
        this.alertType = alertType;
        this.correlationScore = correlationScore;
        this.correlationMetrics = correlationMetrics;
    }
    
    // Getters
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public List<String> getAffectedRegions() { return affectedRegions; }
    public String getAlertType() { return alertType; }
    public double getCorrelationScore() { return correlationScore; }
    public Map<String, Object> getCorrelationMetrics() { return correlationMetrics; }
    
    // Computed properties
    public boolean isHighCorrelation() {
        return correlationScore >= 0.8;
    }
    
    public boolean isCrossRegionPattern() {
        return affectedRegions.size() >= 2 && correlationScore >= 0.6;
    }
    
    public String getCorrelationType() {
        return (String) correlationMetrics.getOrDefault("correlation_type", "UNKNOWN");
    }
    
    public String getPotentialCause() {
        return (String) correlationMetrics.getOrDefault("potential_cause", "Unknown");
    }
}