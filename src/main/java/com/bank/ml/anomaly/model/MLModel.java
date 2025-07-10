package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ML Model
 * Represents a trained machine learning model
 */
public class MLModel {
    
    private final String modelName;
    private final List<String> features;
    private final double accuracy;
    private final double initialAccuracy;
    private final LocalDateTime trainingTime;
    private final String status;
    
    public MLModel(String modelName, List<String> features, double accuracy,
                  double initialAccuracy, LocalDateTime trainingTime, String status) {
        this.modelName = modelName;
        this.features = features;
        this.accuracy = accuracy;
        this.initialAccuracy = initialAccuracy;
        this.trainingTime = trainingTime;
        this.status = status;
    }
    
    // Getters
    public String getModelName() { return modelName; }
    public List<String> getFeatures() { return features; }
    public double getAccuracy() { return accuracy; }
    public double getInitialAccuracy() { return initialAccuracy; }
    public LocalDateTime getTrainingTime() { return trainingTime; }
    public String getStatus() { return status; }
    
    // Computed metrics
    public double getPerformanceDecay() {
        return initialAccuracy - accuracy;
    }
    
    public boolean isActive() {
        return "TRAINED".equals(status) || "ACTIVE".equals(status);
    }
}