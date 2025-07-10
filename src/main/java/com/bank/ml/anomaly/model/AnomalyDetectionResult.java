package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Anomaly Detection Result
 * Represents the result of anomaly detection analysis
 */
public class AnomalyDetectionResult {
    
    private final String anomalyId;
    private final String customerId;
    private final String anomalyType;
    private final String severity;
    private final double confidenceScore;
    private final LocalDateTime detectionTime;
    private final List<String> indicators;
    private final Map<String, Object> metadata;
    
    public AnomalyDetectionResult(String anomalyId, String customerId, String anomalyType,
                                 String severity, double confidenceScore, LocalDateTime detectionTime,
                                 List<String> indicators, Map<String, Object> metadata) {
        this.anomalyId = anomalyId;
        this.customerId = customerId;
        this.anomalyType = anomalyType;
        this.severity = severity;
        this.confidenceScore = confidenceScore;
        this.detectionTime = detectionTime;
        this.indicators = indicators;
        this.metadata = metadata;
    }
    
    // Getters
    public String getAnomalyId() { return anomalyId; }
    public String getCustomerId() { return customerId; }
    public String getAnomalyType() { return anomalyType; }
    public String getSeverity() { return severity; }
    public double getConfidenceScore() { return confidenceScore; }
    public LocalDateTime getDetectionTime() { return detectionTime; }
    public List<String> getIndicators() { return indicators; }
    public Map<String, Object> getMetadata() { return metadata; }
}