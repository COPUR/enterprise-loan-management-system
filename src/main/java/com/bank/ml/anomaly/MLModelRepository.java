package com.bank.ml.anomaly;

import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bank.ml.anomaly.model.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * ML Model Repository
 * Manages ML models for anomaly detection including training, storage, and performance tracking
 */
@Repository
public class MLModelRepository {

    private static final Logger logger = LoggerFactory.getLogger(MLModelRepository.class);
    
    // In-memory storage for models (in production, this would be persistent storage)
    private final Map<String, MLModel> models = new ConcurrentHashMap<>();
    private final Map<String, ModelPerformanceMetrics> performanceMetrics = new ConcurrentHashMap<>();
    
    // Performance thresholds for retraining
    private static final double ACCURACY_THRESHOLD = 0.85;
    private static final double PERFORMANCE_DECAY_THRESHOLD = 0.05;
    private static final int DAYS_BEFORE_MANDATORY_RETRAIN = 30;
    
    /**
     * Train a new ML model
     */
    public MLModelTrainingResult trainModel(TrainingData trainingData) {
        logger.info("Training ML model: {}", trainingData.getModelName());
        
        try {
            // Simulate model training process
            long startTime = System.currentTimeMillis();
            
            // Validate training data
            validateTrainingData(trainingData);
            
            // Simulate training process
            MLModel trainedModel = performTraining(trainingData);
            
            // Store the model
            models.put(trainingData.getModelName(), trainedModel);
            
            long trainingTime = System.currentTimeMillis() - startTime;
            
            // Create training result
            String modelId = generateModelId(trainingData.getModelName());
            Map<String, Object> metrics = buildTrainingMetrics(trainingTime, trainedModel);
            
            // Update performance metrics
            updatePerformanceMetrics(trainingData.getModelName(), trainedModel);
            
            logger.info("Model training completed successfully: {} (accuracy: {})", 
                trainingData.getModelName(), trainedModel.getAccuracy());
            
            return new MLModelTrainingResult(
                modelId,
                trainingData.getModelName(),
                true,
                trainedModel.getAccuracy(),
                LocalDateTime.now(),
                metrics
            );
            
        } catch (Exception e) {
            logger.error("Error training model {}: {}", trainingData.getModelName(), e.getMessage());
            
            return new MLModelTrainingResult(
                "ERROR",
                trainingData.getModelName(),
                false,
                0.0,
                LocalDateTime.now(),
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Get performance metrics for a model
     */
    public ModelPerformanceMetrics getPerformanceMetrics(String modelName) {
        logger.debug("Retrieving performance metrics for model: {}", modelName);
        
        ModelPerformanceMetrics metrics = performanceMetrics.get(modelName);
        
        if (metrics == null) {
            // Create default metrics if not found
            metrics = new ModelPerformanceMetrics(
                modelName,
                0.80, // Default accuracy
                0.78, // Default precision
                0.75, // Default recall
                LocalDateTime.now().minusDays(7)
            );
            
            performanceMetrics.put(modelName, metrics);
        }
        
        return metrics;
    }
    
    /**
     * Determine if a model should be retrained
     */
    public boolean shouldRetrain(String modelName) {
        logger.debug("Checking if model {} should be retrained", modelName);
        
        MLModel model = models.get(modelName);
        ModelPerformanceMetrics metrics = performanceMetrics.get(modelName);
        
        if (model == null || metrics == null) {
            logger.warn("Model {} not found, recommending retraining", modelName);
            return true;
        }
        
        // Check accuracy threshold
        if (metrics.getAccuracy() < ACCURACY_THRESHOLD) {
            logger.info("Model {} accuracy below threshold: {} < {}", 
                modelName, metrics.getAccuracy(), ACCURACY_THRESHOLD);
            return true;
        }
        
        // Check performance decay
        double performanceDecay = model.getInitialAccuracy() - metrics.getAccuracy();
        if (performanceDecay > PERFORMANCE_DECAY_THRESHOLD) {
            logger.info("Model {} performance has decayed: {} > {}", 
                modelName, performanceDecay, PERFORMANCE_DECAY_THRESHOLD);
            return true;
        }
        
        // Check time since last training
        LocalDateTime lastTraining = metrics.getLastUpdated();
        if (lastTraining.isBefore(LocalDateTime.now().minusDays(DAYS_BEFORE_MANDATORY_RETRAIN))) {
            logger.info("Model {} is older than {} days, recommending retraining", 
                modelName, DAYS_BEFORE_MANDATORY_RETRAIN);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get a trained model by name
     */
    public MLModel getModel(String modelName) {
        logger.debug("Retrieving model: {}", modelName);
        
        MLModel model = models.get(modelName);
        
        if (model == null) {
            logger.warn("Model {} not found", modelName);
            throw new MLModelException("Model not found: " + modelName);
        }
        
        return model;
    }
    
    /**
     * Update model performance metrics
     */
    public void updateModelPerformance(String modelName, double accuracy, double precision, double recall) {
        logger.debug("Updating performance metrics for model: {}", modelName);
        
        ModelPerformanceMetrics metrics = new ModelPerformanceMetrics(
            modelName,
            accuracy,
            precision,
            recall,
            LocalDateTime.now()
        );
        
        performanceMetrics.put(modelName, metrics);
    }
    
    /**
     * Delete a model
     */
    public void deleteModel(String modelName) {
        logger.info("Deleting model: {}", modelName);
        
        models.remove(modelName);
        performanceMetrics.remove(modelName);
    }
    
    /**
     * List all available models
     */
    public Map<String, MLModel> getAllModels() {
        return new HashMap<>(models);
    }
    
    /**
     * Validate training data
     */
    private void validateTrainingData(TrainingData trainingData) {
        if (trainingData.getModelName() == null || trainingData.getModelName().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
        
        if (trainingData.getFeatures() == null || trainingData.getFeatures().isEmpty()) {
            throw new IllegalArgumentException("Features cannot be null or empty");
        }
        
        if (trainingData.getTrainingSize() <= 0) {
            throw new IllegalArgumentException("Training size must be positive");
        }
        
        if (trainingData.getTrainingSize() < 100) {
            logger.warn("Training size {} is very small, may result in poor model quality", 
                trainingData.getTrainingSize());
        }
    }
    
    /**
     * Perform model training simulation
     */
    private MLModel performTraining(TrainingData trainingData) {
        // Simulate training process
        try {
            Thread.sleep(100); // Simulate training time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MLModelException("Training interrupted", e);
        }
        
        // Calculate accuracy based on training data size and features
        double baseAccuracy = 0.75;
        double sizeBonus = Math.min(0.15, trainingData.getTrainingSize() / 10000.0);
        double featureBonus = Math.min(0.1, trainingData.getFeatures().size() / 20.0);
        
        double accuracy = baseAccuracy + sizeBonus + featureBonus;
        accuracy = Math.min(0.98, accuracy); // Cap at 98%
        
        // Create model
        return new MLModel(
            trainingData.getModelName(),
            trainingData.getFeatures(),
            accuracy,
            accuracy, // Initial accuracy same as current
            LocalDateTime.now(),
            "TRAINED"
        );
    }
    
    /**
     * Build training metrics
     */
    private Map<String, Object> buildTrainingMetrics(long trainingTime, MLModel model) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("training_time", trainingTime);
        metrics.put("accuracy", model.getAccuracy());
        metrics.put("model_size", model.getFeatures().size());
        metrics.put("training_completion", LocalDateTime.now().toString());
        
        // Simulate additional metrics
        metrics.put("validation_score", Math.max(0.0, model.getAccuracy() - 0.02));
        metrics.put("test_score", Math.max(0.0, model.getAccuracy() - 0.01));
        metrics.put("cross_validation_score", Math.max(0.0, model.getAccuracy() - 0.015));
        
        return metrics;
    }
    
    /**
     * Update performance metrics after training
     */
    private void updatePerformanceMetrics(String modelName, MLModel model) {
        ModelPerformanceMetrics metrics = new ModelPerformanceMetrics(
            modelName,
            model.getAccuracy(),
            Math.max(0.0, model.getAccuracy() - 0.02), // Simulate precision
            Math.max(0.0, model.getAccuracy() - 0.03), // Simulate recall
            LocalDateTime.now()
        );
        
        performanceMetrics.put(modelName, metrics);
    }
    
    /**
     * Generate unique model ID
     */
    private String generateModelId(String modelName) {
        return modelName.toUpperCase() + "_" + System.currentTimeMillis();
    }
}