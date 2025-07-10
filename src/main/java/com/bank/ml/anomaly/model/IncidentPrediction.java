package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Incident Prediction
 * Represents a predicted incident based on historical patterns
 */
public class IncidentPrediction {
    
    private final String predictionId;
    private final LocalDateTime predictedTime;
    private final String incidentType;
    private final double probability;
    private final String severity;
    private final List<String> indicators;
    private final Map<String, Object> predictionMetrics;
    
    public IncidentPrediction(String predictionId, LocalDateTime predictedTime,
                             String incidentType, double probability, String severity,
                             List<String> indicators, Map<String, Object> predictionMetrics) {
        this.predictionId = predictionId;
        this.predictedTime = predictedTime;
        this.incidentType = incidentType;
        this.probability = probability;
        this.severity = severity;
        this.indicators = indicators;
        this.predictionMetrics = predictionMetrics;
    }
    
    // Getters
    public String getPredictionId() { return predictionId; }
    public LocalDateTime getPredictedTime() { return predictedTime; }
    public String getIncidentType() { return incidentType; }
    public double getProbability() { return probability; }
    public String getSeverity() { return severity; }
    public List<String> getIndicators() { return indicators; }
    public Map<String, Object> getPredictionMetrics() { return predictionMetrics; }
}