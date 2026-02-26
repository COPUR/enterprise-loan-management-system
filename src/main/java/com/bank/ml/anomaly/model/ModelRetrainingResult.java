package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model Retraining Result
 * Represents the outcome of ML model retraining
 */
public class ModelRetrainingResult {
    
    private final String modelName;
    private final boolean success;
    private final double newAccuracy;
    private final double previousAccuracy;
    private final LocalDateTime retrainingTime;
    private final Map<String, Object> metrics;
    private final String reason;
    
    public ModelRetrainingResult(String modelName, boolean success, double newAccuracy,
                                double previousAccuracy, LocalDateTime retrainingTime,
                                Map<String, Object> metrics, String reason) {
        this.modelName = modelName;
        this.success = success;
        this.newAccuracy = newAccuracy;
        this.previousAccuracy = previousAccuracy;
        this.retrainingTime = retrainingTime;
        this.metrics = metrics;
        this.reason = reason;
    }
    
    // Getters
    public String getModelName() { return modelName; }
    public boolean isSuccess() { return success; }
    public double getNewAccuracy() { return newAccuracy; }
    public double getPreviousAccuracy() { return previousAccuracy; }
    public LocalDateTime getRetrainingTime() { return retrainingTime; }
    public Map<String, Object> getMetrics() { return metrics; }
    public String getReason() { return reason; }
    
    // Computed metrics
    public double getAccuracyImprovement() {
        return newAccuracy - previousAccuracy;
    }
    
    public double getAccuracyImprovementPercentage() {
        if (previousAccuracy == 0) return 0.0;
        return (getAccuracyImprovement() / previousAccuracy) * 100.0;
    }
}