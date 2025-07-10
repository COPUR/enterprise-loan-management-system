package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * System Performance Anomaly
 * Represents detected anomalies in system performance metrics
 */
public class SystemPerformanceAnomaly {
    
    private final String anomalyId;
    private final LocalDateTime detectionTime;
    private final String anomalyType;
    private final String severity;
    private final double confidenceScore;
    private final SystemMetrics systemMetrics;
    private final List<String> affectedServices;
    private final Map<String, Object> anomalyDetails;
    
    public SystemPerformanceAnomaly(String anomalyId, LocalDateTime detectionTime,
                                   String anomalyType, String severity, double confidenceScore,
                                   SystemMetrics systemMetrics, List<String> affectedServices,
                                   Map<String, Object> anomalyDetails) {
        this.anomalyId = anomalyId;
        this.detectionTime = detectionTime;
        this.anomalyType = anomalyType;
        this.severity = severity;
        this.confidenceScore = confidenceScore;
        this.systemMetrics = systemMetrics;
        this.affectedServices = affectedServices;
        this.anomalyDetails = anomalyDetails;
    }
    
    // Getters
    public String getAnomalyId() { return anomalyId; }
    public LocalDateTime getDetectionTime() { return detectionTime; }
    public String getAnomalyType() { return anomalyType; }
    public String getSeverity() { return severity; }
    public double getConfidenceScore() { return confidenceScore; }
    public SystemMetrics getSystemMetrics() { return systemMetrics; }
    public List<String> getAffectedServices() { return affectedServices; }
    public Map<String, Object> getAnomalyDetails() { return anomalyDetails; }
}