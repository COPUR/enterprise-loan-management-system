package com.bank.ml.anomaly;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bank.ml.anomaly.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * ML-based Anomaly Detection Service
 * Provides comprehensive machine learning anomaly detection for banking operations
 */
@Service
public class MLAnomalyDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(MLAnomalyDetectionService.class);
    
    private final FraudDetectionMLService fraudDetectionMLService;
    private final TransactionPatternAnalyzer transactionPatternAnalyzer;
    private final SystemPerformanceAnomalyDetector systemPerformanceAnomalyDetector;
    private final IncidentPredictionService incidentPredictionService;
    private final MLModelRepository mlModelRepository;
    private final AnomalyDataRepository anomalyDataRepository;
    
    public MLAnomalyDetectionService(
            FraudDetectionMLService fraudDetectionMLService,
            TransactionPatternAnalyzer transactionPatternAnalyzer,
            SystemPerformanceAnomalyDetector systemPerformanceAnomalyDetector,
            IncidentPredictionService incidentPredictionService,
            MLModelRepository mlModelRepository,
            AnomalyDataRepository anomalyDataRepository) {
        
        this.fraudDetectionMLService = fraudDetectionMLService;
        this.transactionPatternAnalyzer = transactionPatternAnalyzer;
        this.systemPerformanceAnomalyDetector = systemPerformanceAnomalyDetector;
        this.incidentPredictionService = incidentPredictionService;
        this.mlModelRepository = mlModelRepository;
        this.anomalyDataRepository = anomalyDataRepository;
    }
    
    /**
     * Detect real-time fraud using ML models
     */
    public FraudDetectionResult detectRealTimeFraud(TransactionData transaction) {
        logger.debug("Detecting fraud for transaction: {}", transaction.getTransactionId());
        
        try {
            FraudDetectionResult result = fraudDetectionMLService.detectFraud(transaction);
            
            if (result.isAnomalous()) {
                logger.warn("Fraud detected for transaction {}: confidence={}, severity={}", 
                    transaction.getTransactionId(), result.getConfidenceScore(), result.getSeverity());
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error detecting fraud for transaction {}: {}", 
                transaction.getTransactionId(), e.getMessage());
            
            return new FraudDetectionResult(
                transaction.getTransactionId(),
                false,
                0.0,
                "UNKNOWN",
                List.of("model_error"),
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Analyze transaction patterns for unusual behavior
     */
    public TransactionPatternAnalysis analyzeTransactionPatterns(String customerId, List<TransactionData> transactions) {
        logger.debug("Analyzing transaction patterns for customer: {}", customerId);
        
        try {
            return transactionPatternAnalyzer.analyzePatterns(customerId, transactions);
            
        } catch (Exception e) {
            logger.error("Error analyzing patterns for customer {}: {}", customerId, e.getMessage());
            
            return new TransactionPatternAnalysis(
                customerId,
                false,
                0.0,
                "UNKNOWN",
                List.of("analysis_error"),
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Detect system performance anomalies
     */
    public SystemPerformanceAnomaly detectSystemPerformanceAnomalies(SystemMetrics metrics) {
        logger.debug("Detecting system performance anomalies for timestamp: {}", metrics.getTimestamp());
        
        try {
            return systemPerformanceAnomalyDetector.detectAnomalies(metrics);
            
        } catch (Exception e) {
            logger.error("Error detecting system performance anomalies: {}", e.getMessage());
            
            return new SystemPerformanceAnomaly(
                "PERF_ERROR",
                metrics.getTimestamp(),
                false,
                0.0,
                "UNKNOWN",
                List.of("detection_error"),
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Predict incidents based on historical patterns
     */
    public List<IncidentPrediction> predictIncidents(HistoricalData historicalData) {
        logger.debug("Predicting incidents for period: {} to {}", 
            historicalData.getStartTime(), historicalData.getEndTime());
        
        try {
            List<IncidentPrediction> predictions = incidentPredictionService.predictIncidents(historicalData);
            
            predictions.stream()
                .filter(p -> p.getProbability() > 0.7)
                .forEach(p -> logger.warn("High probability incident predicted: {} at {} (probability: {})", 
                    p.getPredictedIncidentType(), p.getPredictedTime(), p.getProbability()));
            
            return predictions;
            
        } catch (Exception e) {
            logger.error("Error predicting incidents: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Train ML model with new data
     */
    public MLModelTrainingResult trainModel(TrainingData trainingData) {
        logger.info("Training ML model: {}", trainingData.getModelName());
        
        try {
            MLModelTrainingResult result = mlModelRepository.trainModel(trainingData);
            
            if (result.isTrainingSuccessful()) {
                logger.info("Model {} trained successfully with accuracy: {}", 
                    trainingData.getModelName(), result.getAccuracy());
            } else {
                logger.error("Model {} training failed", trainingData.getModelName());
            }
            
            return result;
            
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
     * Save anomaly detection result
     */
    public AnomalyDetectionResult saveAnomalyResult(AnomalyDetectionResult anomalyResult) {
        logger.debug("Saving anomaly result: {}", anomalyResult.getAnomalyId());
        
        try {
            return anomalyDataRepository.save(anomalyResult);
            
        } catch (Exception e) {
            logger.error("Error saving anomaly result {}: {}", 
                anomalyResult.getAnomalyId(), e.getMessage());
            throw new RuntimeException("Failed to save anomaly result", e);
        }
    }
    
    /**
     * Get anomalies for a specific customer
     */
    public List<AnomalyDetectionResult> getAnomaliesForCustomer(String customerId) {
        logger.debug("Retrieving anomalies for customer: {}", customerId);
        
        try {
            return anomalyDataRepository.findByCustomerId(customerId);
            
        } catch (Exception e) {
            logger.error("Error retrieving anomalies for customer {}: {}", customerId, e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Retrain model if performance has degraded
     */
    public ModelRetrainingResult retrainModelIfNeeded(String modelName, TrainingData retrainingData) {
        logger.info("Checking if model {} needs retraining", modelName);
        
        try {
            ModelPerformanceMetrics currentMetrics = mlModelRepository.getPerformanceMetrics(modelName);
            boolean needsRetraining = mlModelRepository.shouldRetrain(modelName);
            
            if (needsRetraining) {
                logger.info("Model {} performance has degraded, initiating retraining", modelName);
                
                MLModelTrainingResult trainingResult = mlModelRepository.trainModel(retrainingData);
                
                double improvement = trainingResult.getAccuracy() - currentMetrics.getAccuracy();
                
                return new ModelRetrainingResult(
                    modelName,
                    true,
                    trainingResult.getAccuracy(),
                    improvement,
                    trainingResult.getTrainingCompletedAt(),
                    trainingResult.getMetrics()
                );
                
            } else {
                logger.debug("Model {} performance is acceptable, no retraining needed", modelName);
                
                return new ModelRetrainingResult(
                    modelName,
                    false,
                    currentMetrics.getAccuracy(),
                    0.0,
                    LocalDateTime.now(),
                    Map.of("reason", "performance_acceptable")
                );
            }
            
        } catch (Exception e) {
            logger.error("Error checking retraining for model {}: {}", modelName, e.getMessage());
            
            return new ModelRetrainingResult(
                modelName,
                false,
                0.0,
                0.0,
                LocalDateTime.now(),
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Generate comprehensive anomaly report
     */
    public AnomalyReport generateAnomalyReport(LocalDateTime startTime, LocalDateTime endTime) {
        logger.info("Generating anomaly report for period: {} to {}", startTime, endTime);
        
        try {
            List<AnomalyDetectionResult> anomalies = anomalyDataRepository.findByDateRange(startTime, endTime);
            
            int totalAnomalies = anomalies.size();
            int highSeverityCount = (int) anomalies.stream()
                .filter(a -> "HIGH".equals(a.getSeverity()))
                .count();
            int mediumSeverityCount = (int) anomalies.stream()
                .filter(a -> "MEDIUM".equals(a.getSeverity()))
                .count();
            int lowSeverityCount = (int) anomalies.stream()
                .filter(a -> "LOW".equals(a.getSeverity()))
                .count();
            
            List<String> anomalyTypes = anomalies.stream()
                .map(AnomalyDetectionResult::getAnomalyType)
                .distinct()
                .collect(Collectors.toList());
            
            double averageConfidenceScore = anomalies.stream()
                .mapToDouble(AnomalyDetectionResult::getConfidenceScore)
                .average()
                .orElse(0.0);
            
            return new AnomalyReport(
                startTime,
                endTime,
                totalAnomalies,
                highSeverityCount,
                mediumSeverityCount,
                lowSeverityCount,
                anomalyTypes,
                averageConfidenceScore,
                anomalies
            );
            
        } catch (Exception e) {
            logger.error("Error generating anomaly report: {}", e.getMessage());
            
            return new AnomalyReport(
                startTime,
                endTime,
                0,
                0,
                0,
                0,
                List.of(),
                0.0,
                List.of()
            );
        }
    }
    
    /**
     * Process real-time anomaly detection for multiple transactions concurrently
     */
    public CompletableFuture<List<FraudDetectionResult>> detectFraudConcurrently(List<TransactionData> transactions) {
        logger.debug("Processing {} transactions for concurrent fraud detection", transactions.size());
        
        List<CompletableFuture<FraudDetectionResult>> futures = transactions.stream()
            .map(transaction -> CompletableFuture.supplyAsync(() -> detectRealTimeFraud(transaction)))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}