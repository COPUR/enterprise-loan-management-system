package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Training Data
 * Represents data used for training ML models
 */
public class TrainingData {
    
    private final String modelName;
    private final List<String> features;
    private final int trainingSize;
    private final LocalDateTime trainingDate;
    private final String dataSource;
    private final Map<String, Object> trainingParameters;
    private final double validationSplit;
    
    public TrainingData(String modelName, List<String> features, int trainingSize,
                       LocalDateTime trainingDate, String dataSource,
                       Map<String, Object> trainingParameters, double validationSplit) {
        this.modelName = modelName;
        this.features = features;
        this.trainingSize = trainingSize;
        this.trainingDate = trainingDate;
        this.dataSource = dataSource;
        this.trainingParameters = trainingParameters;
        this.validationSplit = validationSplit;
    }
    
    // Getters
    public String getModelName() { return modelName; }
    public List<String> getFeatures() { return features; }
    public int getTrainingSize() { return trainingSize; }
    public LocalDateTime getTrainingDate() { return trainingDate; }
    public String getDataSource() { return dataSource; }
    public Map<String, Object> getTrainingParameters() { return trainingParameters; }
    public double getValidationSplit() { return validationSplit; }
}