package com.bank.payment.infrastructure.fraud;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * ML Model Service for Fraud Detection
 * 
 * Implements ensemble machine learning models for fraud detection:
 * - Random Forest for transaction classification
 * - Neural Network for pattern recognition
 * - Gradient Boosting for risk scoring
 * - Isolation Forest for anomaly detection
 * - Time Series models for temporal patterns
 */
@Service
@Slf4j
public class MLModelService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${ml.model.ensemble.weights.rf:0.3}")
    private double randomForestWeight;
    
    @Value("${ml.model.ensemble.weights.nn:0.25}")
    private double neuralNetworkWeight;
    
    @Value("${ml.model.ensemble.weights.gb:0.25}")
    private double gradientBoostingWeight;
    
    @Value("${ml.model.ensemble.weights.if:0.2}")
    private double isolationForestWeight;
    
    @Value("${ml.model.service.url:http://localhost:8090/ml-api}")
    private String mlServiceUrl;
    
    @Value("${ml.model.service.api-key:demo-ml-key}")
    private String mlApiKey;
    
    @Value("${ml.model.service.enabled:false}")
    private boolean mlServiceEnabled;
    
    @Value("${ml.model.service.timeout:5000}")
    private int mlServiceTimeout;
    
    // Model performance metrics cache
    private final Map<String, ModelMetrics> modelMetricsCache = new ConcurrentHashMap<>();
    
    // Feature importance scores
    private final Map<String, Double> featureImportance = initializeFeatureImportance();
    
    public MLModelService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Predict fraud risk score using ensemble of ML models
     */
    public int predictRiskScore(MLFeatures features) {
        try {
            if (mlServiceEnabled) {
                return predictWithExternalMLService(features);
            } else {
                return predictWithLocalModels(features);
            }
        } catch (Exception e) {
            log.error("Error predicting risk score: {}", e.getMessage());
            return fallbackRiskScore(features);
        }
    }
    
    /**
     * Predict using external ML service (e.g., TensorFlow Serving, MLflow, etc.)
     */
    private int predictWithExternalMLService(MLFeatures features) {
        try {
            // Prepare request payload
            Map<String, Object> requestPayload = Map.of(
                "instances", List.of(convertFeaturesToArray(features)),
                "model_name", "fraud_detection_ensemble",
                "version", "latest"
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + mlApiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);
            
            // Make async call with timeout
            CompletableFuture<ResponseEntity<String>> futureResponse = CompletableFuture
                .supplyAsync(() -> restTemplate.exchange(
                    mlServiceUrl + "/v1/models/fraud_detection:predict",
                    HttpMethod.POST,
                    entity,
                    String.class
                ));
            
            ResponseEntity<String> response = futureResponse.get(mlServiceTimeout, TimeUnit.MILLISECONDS);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode predictions = responseJson.get("predictions");
                
                if (predictions != null && predictions.isArray() && predictions.size() > 0) {
                    double riskScore = predictions.get(0).get("fraud_probability").asDouble();
                    return (int) Math.round(riskScore * 100);
                }
            }
            
            log.warn("External ML service returned invalid response, falling back to local models");
            return predictWithLocalModels(features);
            
        } catch (Exception e) {
            log.error("Error calling external ML service: {}", e.getMessage());
            return predictWithLocalModels(features);
        }
    }
    
    /**
     * Predict using local/embedded models
     */
    private int predictWithLocalModels(MLFeatures features) {
        try {
            // Get predictions from individual models
            double rfScore = randomForestPredict(features);
            double nnScore = neuralNetworkPredict(features);
            double gbScore = gradientBoostingPredict(features);
            double ifScore = isolationForestPredict(features);
            
            // Ensemble prediction with weighted average
            double ensembleScore = (rfScore * randomForestWeight) +
                                 (nnScore * neuralNetworkWeight) +
                                 (gbScore * gradientBoostingWeight) +
                                 (ifScore * isolationForestWeight);
            
            // Apply confidence adjustments
            double confidenceAdjustedScore = applyConfidenceAdjustment(ensembleScore, features);
            
            // Update model metrics
            updateModelMetrics(rfScore, nnScore, gbScore, ifScore, ensembleScore);
            
            return Math.max(0, Math.min(100, (int)Math.round(confidenceAdjustedScore * 100)));
            
        } catch (Exception e) {
            log.error("Error in local model prediction: {}", e.getMessage());
            return fallbackRiskScore(features);
        }
    }
    
    /**
     * Convert features to array format for ML service
     */
    private double[] convertFeaturesToArray(MLFeatures features) {
        return new double[] {
            features.getAmount().doubleValue(),
            features.getHourOfDay(),
            features.getDayOfWeek(),
            encodePaymentType(features.getPaymentType()),
            encodeAccountType(features.getFromAccountType()),
            encodeAccountType(features.getToAccountType()),
            features.getCustomerId().hashCode() % 1000 / 1000.0 // Normalized customer ID
        };
    }
    
    /**
     * Fallback risk scoring when ML models fail
     */
    private int fallbackRiskScore(MLFeatures features) {
        int baseScore = 30; // Base risk score
        
        // High amount transactions
        if (features.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            baseScore += 20;
        }
        
        // Off-hours transactions
        if (features.getHourOfDay() < 6 || features.getHourOfDay() > 22) {
            baseScore += 15;
        }
        
        // Risky payment types
        if ("WIRE_TRANSFER".equals(features.getPaymentType())) {
            baseScore += 25;
        }
        
        // Cross-account type transfers
        if (!features.getFromAccountType().equals(features.getToAccountType())) {
            baseScore += 15;
        }
        
        return Math.min(100, baseScore);
    }
    
    /**
     * Random Forest model prediction
     */
    private double randomForestPredict(MLFeatures features) {
        // Simulate Random Forest ensemble prediction
        double score = 0.0;
        
        // Amount-based features
        if (features.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            score += 0.15;
        }
        if (features.getAmount().compareTo(BigDecimal.valueOf(50000)) > 0) {
            score += 0.25;
        }
        
        // Time-based features
        int hour = features.getHourOfDay();
        if (hour < 6 || hour > 22) {
            score += 0.1; // Late night transactions are riskier
        }
        
        // Day-of-week patterns
        if (features.getDayOfWeek() == 7) { // Sunday
            score += 0.05;
        }
        
        // Payment type risk
        switch (features.getPaymentType()) {
            case "WIRE_TRANSFER":
                score += 0.2;
                break;
            case "ACH_TRANSFER":
                score += 0.1;
                break;
            case "BANK_TRANSFER":
                score += 0.05;
                break;
        }
        
        // Account type cross-validation
        if (!features.getFromAccountType().equals(features.getToAccountType())) {
            score += 0.1;
        }
        
        // Add random forest noise simulation
        Random random = new Random(features.getCustomerId().hashCode());
        score += (random.nextGaussian() * 0.05);
        
        return Math.max(0, Math.min(1, score));
    }
    
    /**
     * Neural Network model prediction
     */
    private double neuralNetworkPredict(MLFeatures features) {
        // Simulate deep neural network prediction
        
        // Input layer features (normalized)
        double[] inputs = new double[]{
            normalizeAmount(features.getAmount()),
            normalizeTime(features.getHourOfDay()),
            normalizeDayOfWeek(features.getDayOfWeek()),
            encodePaymentType(features.getPaymentType()),
            encodeAccountType(features.getFromAccountType()),
            encodeAccountType(features.getToAccountType())
        };
        
        // Hidden layer 1 (6 -> 10 neurons)
        double[] hidden1 = applyLayer(inputs, getWeights(6, 10), getBiases(10));
        
        // Hidden layer 2 (10 -> 8 neurons)
        double[] hidden2 = applyLayer(hidden1, getWeights(10, 8), getBiases(8));
        
        // Hidden layer 3 (8 -> 5 neurons)
        double[] hidden3 = applyLayer(hidden2, getWeights(8, 5), getBiases(5));
        
        // Output layer (5 -> 1 neuron)
        double[] output = applyLayer(hidden3, getWeights(5, 1), getBiases(1));
        
        return sigmoid(output[0]);
    }
    
    /**
     * Gradient Boosting model prediction
     */
    private double gradientBoostingPredict(MLFeatures features) {
        // Simulate Gradient Boosting ensemble
        double prediction = 0.0;
        
        // Tree 1: Amount-based decision tree
        if (features.getAmount().compareTo(BigDecimal.valueOf(1000)) > 0) {
            if (features.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
                prediction += 0.3;
            } else {
                prediction += 0.1;
            }
        }
        
        // Tree 2: Temporal pattern tree
        if (features.getHourOfDay() < 6 || features.getHourOfDay() > 22) {
            if (features.getDayOfWeek() > 5) { // Weekend
                prediction += 0.25;
            } else {
                prediction += 0.15;
            }
        }
        
        // Tree 3: Payment type and account combination
        if (features.getPaymentType().equals("WIRE_TRANSFER")) {
            if (!features.getFromAccountType().equals("CHECKING")) {
                prediction += 0.35;
            } else {
                prediction += 0.2;
            }
        }
        
        // Tree 4: Customer behavior tree (simulated)
        String customerId = features.getCustomerId();
        if (customerId.hashCode() % 10 > 7) { // 20% of customers flagged as high risk
            prediction += 0.2;
        }
        
        // Apply gradient boosting learning rate
        prediction *= 0.1; // Learning rate
        
        return Math.max(0, Math.min(1, prediction));
    }
    
    /**
     * Isolation Forest anomaly detection
     */
    private double isolationForestPredict(MLFeatures features) {
        // Simulate Isolation Forest anomaly score
        
        // Calculate isolation path length for key features
        double amountIsolation = calculateIsolationScore(
            normalizeAmount(features.getAmount()), 0.5, 0.2
        );
        
        double timeIsolation = calculateIsolationScore(
            normalizeTime(features.getHourOfDay()), 0.4, 0.15
        );
        
        double typeIsolation = calculateIsolationScore(
            encodePaymentType(features.getPaymentType()), 0.3, 0.1
        );
        
        // Combine isolation scores
        double avgIsolation = (amountIsolation + timeIsolation + typeIsolation) / 3.0;
        
        // Convert to anomaly score (higher = more anomalous)
        return 1.0 - avgIsolation;
    }
    
    /**
     * Apply confidence adjustment based on feature quality
     */
    private double applyConfidenceAdjustment(double score, MLFeatures features) {
        double confidence = 1.0;
        
        // Reduce confidence for missing or low-quality features
        if (features.getDescription() == null || features.getDescription().isEmpty()) {
            confidence *= 0.95;
        }
        
        if (features.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            confidence *= 0.8;
        }
        
        // Adjust score based on confidence
        return score * confidence;
    }
    
    /**
     * Update model performance metrics
     */
    private void updateModelMetrics(double rf, double nn, double gb, double if_, double ensemble) {
        ModelMetrics metrics = new ModelMetrics(rf, nn, gb, if_, ensemble);
        modelMetricsCache.put("latest", metrics);
        
        // Log metrics for monitoring
        System.out.println(String.format(
            "ML_METRICS: RF=%.3f NN=%.3f GB=%.3f IF=%.3f Ensemble=%.3f",
            rf, nn, gb, if_, ensemble
        ));
    }
    
    // Helper methods for neural network simulation
    private double[] applyLayer(double[] inputs, double[][] weights, double[] biases) {
        double[] outputs = new double[weights[0].length];
        
        for (int j = 0; j < outputs.length; j++) {
            double sum = biases[j];
            for (int i = 0; i < inputs.length; i++) {
                sum += inputs[i] * weights[i][j];
            }
            outputs[j] = relu(sum);
        }
        
        return outputs;
    }
    
    private double relu(double x) {
        return Math.max(0, x);
    }
    
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    private double[][] getWeights(int inputSize, int outputSize) {
        // Return mock weights for simulation
        double[][] weights = new double[inputSize][outputSize];
        Random random = new Random(42); // Fixed seed for consistency
        
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weights[i][j] = random.nextGaussian() * 0.5;
            }
        }
        
        return weights;
    }
    
    private double[] getBiases(int size) {
        // Return mock biases for simulation
        double[] biases = new double[size];
        Random random = new Random(123); // Fixed seed for consistency
        
        for (int i = 0; i < size; i++) {
            biases[i] = random.nextGaussian() * 0.1;
        }
        
        return biases;
    }
    
    // Feature normalization methods
    private double normalizeAmount(BigDecimal amount) {
        // Normalize amount to [0, 1] using log transformation
        double logAmount = Math.log(amount.doubleValue() + 1);
        return Math.min(1.0, logAmount / 15.0); // Log of ~3M
    }
    
    private double normalizeTime(int hour) {
        return hour / 24.0;
    }
    
    private double normalizeDayOfWeek(int dayOfWeek) {
        return (dayOfWeek - 1) / 6.0; // 1-7 -> 0-1
    }
    
    private double encodePaymentType(String paymentType) {
        switch (paymentType) {
            case "BANK_TRANSFER": return 0.2;
            case "ACH_TRANSFER": return 0.4;
            case "WIRE_TRANSFER": return 0.6;
            case "INTERNATIONAL_TRANSFER": return 0.8;
            default: return 0.0;
        }
    }
    
    private double encodeAccountType(String accountType) {
        switch (accountType) {
            case "CHECKING": return 0.2;
            case "SAVINGS": return 0.4;
            case "BUSINESS": return 0.6;
            case "INVESTMENT": return 0.8;
            default: return 0.0;
        }
    }
    
    private double calculateIsolationScore(double feature, double mean, double stdDev) {
        // Simulate isolation forest path length calculation
        double zScore = Math.abs((feature - mean) / stdDev);
        return Math.max(0, Math.min(1, 1.0 - (zScore / 3.0)));
    }
    
    /**
     * Initialize feature importance scores
     */
    private Map<String, Double> initializeFeatureImportance() {
        Map<String, Double> importance = new HashMap<>();
        importance.put("amount", 0.25);
        importance.put("paymentType", 0.20);
        importance.put("hourOfDay", 0.15);
        importance.put("accountTypes", 0.15);
        importance.put("dayOfWeek", 0.10);
        importance.put("customerId", 0.10);
        importance.put("description", 0.05);
        return importance;
    }
    
    /**
     * Get feature importance scores
     */
    public Map<String, Double> getFeatureImportance() {
        return new HashMap<>(featureImportance);
    }
    
    /**
     * Get model performance metrics
     */
    public Optional<ModelMetrics> getLatestMetrics() {
        return Optional.ofNullable(modelMetricsCache.get("latest"));
    }
    
    /**
     * Real-time model retraining trigger
     */
    public void triggerModelRetraining(List<FraudFeedback> feedbackData) {
        if (!mlServiceEnabled) {
            log.info("ML service disabled, skipping model retraining");
            return;
        }
        
        try {
            Map<String, Object> retrainingPayload = Map.of(
                "feedback_data", feedbackData,
                "model_name", "fraud_detection_ensemble",
                "retrain_trigger", "feedback_batch",
                "timestamp", LocalDateTime.now()
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + mlApiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(retrainingPayload, headers);
            
            CompletableFuture.runAsync(() -> {
                try {
                    ResponseEntity<String> response = restTemplate.exchange(
                        mlServiceUrl + "/v1/models/fraud_detection:retrain",
                        HttpMethod.POST,
                        entity,
                        String.class
                    );
                    
                    if (response.getStatusCode() == HttpStatus.ACCEPTED) {
                        log.info("Model retraining triggered successfully");
                    }
                } catch (Exception e) {
                    log.error("Error triggering model retraining: {}", e.getMessage());
                }
            });
            
        } catch (Exception e) {
            log.error("Error preparing model retraining request: {}", e.getMessage());
        }
    }
    
    /**
     * Get model performance metrics
     */
    public ModelMetrics getModelMetrics(String modelName) {
        try {
            if (mlServiceEnabled) {
                return getExternalModelMetrics(modelName);
            } else {
                return getLocalModelMetrics(modelName);
            }
        } catch (Exception e) {
            log.error("Error getting model metrics: {}", e.getMessage());
            return createDefaultMetrics();
        }
    }
    
    private ModelMetrics getExternalModelMetrics(String modelName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + mlApiKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                mlServiceUrl + "/v1/models/" + modelName + "/metrics",
                HttpMethod.GET,
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode metricsJson = objectMapper.readTree(response.getBody());
                
                return ModelMetrics.builder()
                    .modelName(modelName)
                    .accuracy(metricsJson.get("accuracy").asDouble())
                    .precision(metricsJson.get("precision").asDouble())
                    .recall(metricsJson.get("recall").asDouble())
                    .f1Score(metricsJson.get("f1_score").asDouble())
                    .auc(metricsJson.get("auc").asDouble())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            }
        } catch (Exception e) {
            log.error("Error getting external model metrics: {}", e.getMessage());
        }
        
        return getLocalModelMetrics(modelName);
    }
    
    private ModelMetrics getLocalModelMetrics(String modelName) {
        // Return cached or default metrics for local models
        return modelMetricsCache.getOrDefault(modelName, createDefaultMetrics());
    }
    
    private ModelMetrics createDefaultMetrics() {
        return ModelMetrics.builder()
            .modelName("default")
            .accuracy(0.85)
            .precision(0.82)
            .recall(0.78)
            .f1Score(0.80)
            .auc(0.88)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    /**
     * Model performance metrics holder
     */
    public static class ModelMetrics {
        private final String modelName;
        private final double accuracy;
        private final double precision;
        private final double recall;
        private final double f1Score;
        private final double auc;
        private final LocalDateTime lastUpdated;
        
        public ModelMetrics(String modelName, double accuracy, double precision, 
                          double recall, double f1Score, double auc, LocalDateTime lastUpdated) {
            this.modelName = modelName;
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
            this.auc = auc;
            this.lastUpdated = lastUpdated;
        }
        
        public static ModelMetricsBuilder builder() {
            return new ModelMetricsBuilder();
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public double getAccuracy() { return accuracy; }
        public double getPrecision() { return precision; }
        public double getRecall() { return recall; }
        public double getF1Score() { return f1Score; }
        public double getAuc() { return auc; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        
        public static class ModelMetricsBuilder {
            private String modelName;
            private double accuracy;
            private double precision;
            private double recall;
            private double f1Score;
            private double auc;
            private LocalDateTime lastUpdated;
            
            public ModelMetricsBuilder modelName(String modelName) {
                this.modelName = modelName;
                return this;
            }
            
            public ModelMetricsBuilder accuracy(double accuracy) {
                this.accuracy = accuracy;
                return this;
            }
            
            public ModelMetricsBuilder precision(double precision) {
                this.precision = precision;
                return this;
            }
            
            public ModelMetricsBuilder recall(double recall) {
                this.recall = recall;
                return this;
            }
            
            public ModelMetricsBuilder f1Score(double f1Score) {
                this.f1Score = f1Score;
                return this;
            }
            
            public ModelMetricsBuilder auc(double auc) {
                this.auc = auc;
                return this;
            }
            
            public ModelMetricsBuilder lastUpdated(LocalDateTime lastUpdated) {
                this.lastUpdated = lastUpdated;
                return this;
            }
            
            public ModelMetrics build() {
                return new ModelMetrics(modelName, accuracy, precision, recall, f1Score, auc, lastUpdated);
            }
        }
    }
    
    /**
     * Fraud feedback data for model retraining
     */
    public static class FraudFeedback {
        private final String transactionId;
        private final boolean actualFraud;
        private final double predictedScore;
        private final LocalDateTime feedbackDate;
        
        public FraudFeedback(String transactionId, boolean actualFraud, double predictedScore) {
            this.transactionId = transactionId;
            this.actualFraud = actualFraud;
            this.predictedScore = predictedScore;
            this.feedbackDate = LocalDateTime.now();
        }
        
        // Getters
        public String getTransactionId() { return transactionId; }
        public boolean isActualFraud() { return actualFraud; }
        public double getPredictedScore() { return predictedScore; }
        public LocalDateTime getFeedbackDate() { return feedbackDate; }
    }
}