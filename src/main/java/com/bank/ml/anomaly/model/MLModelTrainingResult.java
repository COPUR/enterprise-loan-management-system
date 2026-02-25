package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ML Model Training Result
 * Represents the outcome of ML model training
 */
public class MLModelTrainingResult {
    
    private final String modelId;
    private final String modelName;
    private final boolean success;
    private final double accuracy;
    private final LocalDateTime trainingTime;
    private final Map<String, Object> metrics;
    
    public MLModelTrainingResult(String modelId, String modelName, boolean success,
                                double accuracy, LocalDateTime trainingTime,
                                Map<String, Object> metrics) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.success = success;
        this.accuracy = accuracy;
        this.trainingTime = trainingTime;
        this.metrics = metrics;
    }
    
    // Getters
    public String getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public boolean isSuccess() { return success; }
    public double getAccuracy() { return accuracy; }
    public LocalDateTime getTrainingTime() { return trainingTime; }
    public Map<String, Object> getMetrics() { return metrics; }
}