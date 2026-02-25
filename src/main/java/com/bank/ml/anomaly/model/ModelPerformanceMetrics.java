package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;

/**
 * Model Performance Metrics
 * Tracks ML model performance over time
 */
public class ModelPerformanceMetrics {
    
    private final String modelName;
    private final double accuracy;
    private final double precision;
    private final double recall;
    private final LocalDateTime lastUpdated;
    
    public ModelPerformanceMetrics(String modelName, double accuracy, double precision,
                                  double recall, LocalDateTime lastUpdated) {
        this.modelName = modelName;
        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters
    public String getModelName() { return modelName; }
    public double getAccuracy() { return accuracy; }
    public double getPrecision() { return precision; }
    public double getRecall() { return recall; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    
    // Computed metrics
    public double getF1Score() {
        if (precision + recall == 0) return 0.0;
        return 2 * (precision * recall) / (precision + recall);
    }
}