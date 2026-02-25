package com.bank.ml.anomaly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for ML-based Anomaly Detection Service
 * Tests machine learning anomaly detection for predictive monitoring
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ML Anomaly Detection Service - TDD Test Suite")
class MLAnomalyDetectionServiceTest {

    @Mock
    private FraudDetectionMLService fraudDetectionMLService;
    
    @Mock
    private TransactionPatternAnalyzer transactionPatternAnalyzer;
    
    @Mock
    private SystemPerformanceAnomalyDetector systemPerformanceAnomalyDetector;
    
    @Mock
    private IncidentPredictionService incidentPredictionService;
    
    @Mock
    private MLModelRepository mlModelRepository;
    
    @Mock
    private AnomalyDataRepository anomalyDataRepository;
    
    private MLAnomalyDetectionService mlAnomalyDetectionService;
    
    @BeforeEach
    void setUp() {
        mlAnomalyDetectionService = new MLAnomalyDetectionService(
            fraudDetectionMLService,
            transactionPatternAnalyzer,
            systemPerformanceAnomalyDetector,
            incidentPredictionService,
            mlModelRepository,
            anomalyDataRepository
        );
    }
    
    @Test
    @DisplayName("Should detect real-time fraud with high accuracy")
    void shouldDetectRealTimeFraudWithHighAccuracy() {
        // Given
        TransactionData transaction = new TransactionData(
            "TXN123456",
            "ACC001",
            "ACC002",
            10000.00,
            LocalDateTime.now(),
            "WIRE_TRANSFER",
            Map.of("location", "overseas", "time", "03:00")
        );
        
        FraudDetectionResult expected = new FraudDetectionResult(
            "TXN123456",
            true,
            0.95,
            "HIGH",
            List.of("unusual_amount", "suspicious_time", "overseas_location"),
            Map.of("risk_score", 95.0, "confidence", 0.95)
        );
        
        when(fraudDetectionMLService.detectFraud(transaction)).thenReturn(expected);
        
        // When
        FraudDetectionResult result = mlAnomalyDetectionService.detectRealTimeFraud(transaction);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAnomalous()).isTrue();
        assertThat(result.getConfidenceScore()).isEqualTo(0.95);
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        assertThat(result.getAnomalyFeatures()).containsExactly("unusual_amount", "suspicious_time", "overseas_location");
        assertThat(result.getMetrics().get("risk_score")).isEqualTo(95.0);
        
        verify(fraudDetectionMLService).detectFraud(transaction);
    }
    
    @Test
    @DisplayName("Should identify unusual transaction patterns")
    void shouldIdentifyUnusualTransactionPatterns() {
        // Given
        String customerId = "CUST001";
        List<TransactionData> transactions = List.of(
            new TransactionData("TXN1", "ACC001", "ACC002", 1000.00, LocalDateTime.now().minusHours(1), "TRANSFER", Map.of()),
            new TransactionData("TXN2", "ACC001", "ACC003", 2000.00, LocalDateTime.now().minusHours(2), "TRANSFER", Map.of()),
            new TransactionData("TXN3", "ACC001", "ACC004", 5000.00, LocalDateTime.now().minusHours(3), "TRANSFER", Map.of())
        );
        
        TransactionPatternAnalysis expected = new TransactionPatternAnalysis(
            customerId,
            true,
            0.87,
            "UNUSUAL_FREQUENCY",
            List.of("rapid_consecutive_transfers", "increasing_amounts"),
            Map.of("frequency_score", 8.5, "amount_variance", 0.75)
        );
        
        when(transactionPatternAnalyzer.analyzePatterns(customerId, transactions)).thenReturn(expected);
        
        // When
        TransactionPatternAnalysis result = mlAnomalyDetectionService.analyzeTransactionPatterns(customerId, transactions);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAnomalous()).isTrue();
        assertThat(result.getConfidenceScore()).isEqualTo(0.87);
        assertThat(result.getPatternType()).isEqualTo("UNUSUAL_FREQUENCY");
        assertThat(result.getAnomalyFeatures()).containsExactly("rapid_consecutive_transfers", "increasing_amounts");
        
        verify(transactionPatternAnalyzer).analyzePatterns(customerId, transactions);
    }
    
    @Test
    @DisplayName("Should detect system performance anomalies")
    void shouldDetectSystemPerformanceAnomalies() {
        // Given
        SystemMetrics metrics = new SystemMetrics(
            LocalDateTime.now(),
            85.5, // CPU usage
            78.2, // Memory usage
            450L, // Response time
            125,  // Request rate
            Map.of("disk_usage", 65.0, "network_latency", 250.0)
        );
        
        SystemPerformanceAnomaly expected = new SystemPerformanceAnomaly(
            "PERF001",
            LocalDateTime.now(),
            true,
            0.92,
            "PERFORMANCE_DEGRADATION",
            List.of("high_cpu_usage", "elevated_response_time"),
            Map.of("baseline_deviation", 2.5, "trend_score", 0.8)
        );
        
        when(systemPerformanceAnomalyDetector.detectAnomalies(metrics)).thenReturn(expected);
        
        // When
        SystemPerformanceAnomaly result = mlAnomalyDetectionService.detectSystemPerformanceAnomalies(metrics);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAnomalous()).isTrue();
        assertThat(result.getConfidenceScore()).isEqualTo(0.92);
        assertThat(result.getAnomalyType()).isEqualTo("PERFORMANCE_DEGRADATION");
        assertThat(result.getAnomalyFeatures()).containsExactly("high_cpu_usage", "elevated_response_time");
        
        verify(systemPerformanceAnomalyDetector).detectAnomalies(metrics);
    }
    
    @Test
    @DisplayName("Should predict incidents based on historical patterns")
    void shouldPredictIncidentsBasedOnHistoricalPatterns() {
        // Given
        HistoricalData historicalData = new HistoricalData(
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now(),
            List.of("loan_processing", "payment_system", "fraud_detection"),
            Map.of("incidents", 15, "patterns", List.of("weekend_peaks", "month_end_spikes"))
        );
        
        IncidentPrediction expected = new IncidentPrediction(
            "INC_PRED_001",
            LocalDateTime.now().plusHours(2),
            "PAYMENT_SYSTEM_OVERLOAD",
            0.78,
            "MEDIUM",
            List.of("payment_volume_spike", "processing_delay_pattern"),
            Map.of("probability", 0.78, "time_window", 2.0, "impact_score", 6.5)
        );
        
        when(incidentPredictionService.predictIncidents(historicalData)).thenReturn(List.of(expected));
        
        // When
        List<IncidentPrediction> results = mlAnomalyDetectionService.predictIncidents(historicalData);
        
        // Then
        assertThat(results).hasSize(1);
        IncidentPrediction result = results.get(0);
        assertThat(result.getPredictedIncidentType()).isEqualTo("PAYMENT_SYSTEM_OVERLOAD");
        assertThat(result.getProbability()).isEqualTo(0.78);
        assertThat(result.getSeverity()).isEqualTo("MEDIUM");
        assertThat(result.getIndicators()).containsExactly("payment_volume_spike", "processing_delay_pattern");
        
        verify(incidentPredictionService).predictIncidents(historicalData);
    }
    
    @Test
    @DisplayName("Should train ML models with new data")
    void shouldTrainMLModelsWithNewData() {
        // Given
        TrainingData trainingData = new TrainingData(
            "fraud_detection_v2",
            List.of("transaction_amount", "location", "time", "frequency"),
            1000,
            Map.of("accuracy", 0.94, "precision", 0.92, "recall", 0.89)
        );
        
        MLModelTrainingResult expected = new MLModelTrainingResult(
            "MODEL_FRAUD_V2",
            "fraud_detection_v2",
            true,
            0.94,
            LocalDateTime.now(),
            Map.of("training_time", 45.0, "validation_score", 0.91, "test_score", 0.93)
        );
        
        when(mlModelRepository.trainModel(trainingData)).thenReturn(expected);
        
        // When
        MLModelTrainingResult result = mlAnomalyDetectionService.trainModel(trainingData);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isTrainingSuccessful()).isTrue();
        assertThat(result.getAccuracy()).isEqualTo(0.94);
        assertThat(result.getModelName()).isEqualTo("fraud_detection_v2");
        assertThat(result.getMetrics().get("validation_score")).isEqualTo(0.91);
        
        verify(mlModelRepository).trainModel(trainingData);
    }
    
    @Test
    @DisplayName("Should handle concurrent anomaly detection requests")
    void shouldHandleConcurrentAnomalyDetectionRequests() {
        // Given
        TransactionData transaction1 = new TransactionData("TXN001", "ACC001", "ACC002", 1000.00, LocalDateTime.now(), "TRANSFER", Map.of());
        TransactionData transaction2 = new TransactionData("TXN002", "ACC003", "ACC004", 2000.00, LocalDateTime.now(), "TRANSFER", Map.of());
        
        FraudDetectionResult result1 = new FraudDetectionResult("TXN001", false, 0.15, "LOW", List.of(), Map.of());
        FraudDetectionResult result2 = new FraudDetectionResult("TXN002", true, 0.89, "HIGH", List.of("unusual_amount"), Map.of());
        
        when(fraudDetectionMLService.detectFraud(transaction1)).thenReturn(result1);
        when(fraudDetectionMLService.detectFraud(transaction2)).thenReturn(result2);
        
        // When
        CompletableFuture<FraudDetectionResult> future1 = CompletableFuture.supplyAsync(() -> 
            mlAnomalyDetectionService.detectRealTimeFraud(transaction1)
        );
        CompletableFuture<FraudDetectionResult> future2 = CompletableFuture.supplyAsync(() -> 
            mlAnomalyDetectionService.detectRealTimeFraud(transaction2)
        );
        
        CompletableFuture.allOf(future1, future2).join();
        
        // Then
        assertThat(future1.get().isAnomalous()).isFalse();
        assertThat(future2.get().isAnomalous()).isTrue();
        
        verify(fraudDetectionMLService).detectFraud(transaction1);
        verify(fraudDetectionMLService).detectFraud(transaction2);
    }
    
    @Test
    @DisplayName("Should store and retrieve anomaly detection results")
    void shouldStoreAndRetrieveAnomalyDetectionResults() {
        // Given
        String customerId = "CUST001";
        AnomalyDetectionResult anomalyResult = new AnomalyDetectionResult(
            "ANOMALY_001",
            customerId,
            "FRAUD_DETECTION",
            true,
            0.92,
            "HIGH",
            LocalDateTime.now(),
            List.of("unusual_transaction_pattern"),
            Map.of("risk_score", 92.0)
        );
        
        when(anomalyDataRepository.save(anomalyResult)).thenReturn(anomalyResult);
        when(anomalyDataRepository.findByCustomerId(customerId)).thenReturn(List.of(anomalyResult));
        
        // When
        AnomalyDetectionResult saved = mlAnomalyDetectionService.saveAnomalyResult(anomalyResult);
        List<AnomalyDetectionResult> retrieved = mlAnomalyDetectionService.getAnomaliesForCustomer(customerId);
        
        // Then
        assertThat(saved).isEqualTo(anomalyResult);
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).getCustomerId()).isEqualTo(customerId);
        assertThat(retrieved.get(0).isAnomalous()).isTrue();
        
        verify(anomalyDataRepository).save(anomalyResult);
        verify(anomalyDataRepository).findByCustomerId(customerId);
    }
    
    @Test
    @DisplayName("Should handle model retraining with performance monitoring")
    void shouldHandleModelRetrainingWithPerformanceMonitoring() {
        // Given
        String modelName = "fraud_detection_v2";
        ModelPerformanceMetrics currentMetrics = new ModelPerformanceMetrics(
            modelName,
            0.85, // Current accuracy
            0.82, // Precision
            0.78, // Recall
            LocalDateTime.now().minusDays(1)
        );
        
        ModelPerformanceMetrics expectedMetrics = new ModelPerformanceMetrics(
            modelName,
            0.91, // Improved accuracy
            0.89, // Improved precision
            0.87, // Improved recall
            LocalDateTime.now()
        );
        
        when(mlModelRepository.getPerformanceMetrics(modelName)).thenReturn(currentMetrics);
        when(mlModelRepository.shouldRetrain(modelName)).thenReturn(true);
        
        TrainingData retrainingData = new TrainingData(
            modelName,
            List.of("transaction_amount", "location", "time", "frequency", "merchant_category"),
            1500,
            Map.of("accuracy", 0.91, "precision", 0.89, "recall", 0.87)
        );
        
        MLModelTrainingResult retrainingResult = new MLModelTrainingResult(
            "MODEL_FRAUD_V2_RETRAINED",
            modelName,
            true,
            0.91,
            LocalDateTime.now(),
            Map.of("improvement", 0.06, "training_time", 62.0)
        );
        
        when(mlModelRepository.trainModel(any(TrainingData.class))).thenReturn(retrainingResult);
        
        // When
        ModelRetrainingResult result = mlAnomalyDetectionService.retrainModelIfNeeded(modelName, retrainingData);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isRetrainingPerformed()).isTrue();
        assertThat(result.getPerformanceImprovement()).isGreaterThan(0.0);
        assertThat(result.getNewAccuracy()).isEqualTo(0.91);
        
        verify(mlModelRepository).getPerformanceMetrics(modelName);
        verify(mlModelRepository).shouldRetrain(modelName);
        verify(mlModelRepository).trainModel(any(TrainingData.class));
    }
    
    @Test
    @DisplayName("Should generate comprehensive anomaly reports")
    void shouldGenerateComprehensiveAnomalyReports() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        
        List<AnomalyDetectionResult> anomalies = List.of(
            new AnomalyDetectionResult("ANOMALY_001", "CUST001", "FRAUD_DETECTION", true, 0.92, "HIGH", startTime.plusDays(1), List.of("unusual_pattern"), Map.of()),
            new AnomalyDetectionResult("ANOMALY_002", "CUST002", "PERFORMANCE_DEGRADATION", true, 0.78, "MEDIUM", startTime.plusDays(2), List.of("slow_response"), Map.of()),
            new AnomalyDetectionResult("ANOMALY_003", "CUST003", "TRANSACTION_PATTERN", true, 0.85, "HIGH", startTime.plusDays(3), List.of("frequency_spike"), Map.of())
        );
        
        when(anomalyDataRepository.findByDateRange(startTime, endTime)).thenReturn(anomalies);
        
        // When
        AnomalyReport report = mlAnomalyDetectionService.generateAnomalyReport(startTime, endTime);
        
        // Then
        assertThat(report).isNotNull();
        assertThat(report.getTotalAnomalies()).isEqualTo(3);
        assertThat(report.getHighSeverityCount()).isEqualTo(2);
        assertThat(report.getMediumSeverityCount()).isEqualTo(1);
        assertThat(report.getLowSeverityCount()).isEqualTo(0);
        assertThat(report.getAnomalyTypes()).containsExactlyInAnyOrder("FRAUD_DETECTION", "PERFORMANCE_DEGRADATION", "TRANSACTION_PATTERN");
        assertThat(report.getAverageConfidenceScore()).isEqualTo(0.85);
        
        verify(anomalyDataRepository).findByDateRange(startTime, endTime);
    }
    
    @Test
    @DisplayName("Should handle ML model failures gracefully")
    void shouldHandleMLModelFailuresGracefully() {
        // Given
        TransactionData transaction = new TransactionData("TXN123", "ACC001", "ACC002", 1000.00, LocalDateTime.now(), "TRANSFER", Map.of());
        
        when(fraudDetectionMLService.detectFraud(transaction))
            .thenThrow(new MLModelException("Model inference failed"));
        
        // When
        FraudDetectionResult result = mlAnomalyDetectionService.detectRealTimeFraud(transaction);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isAnomalous()).isFalse();
        assertThat(result.getConfidenceScore()).isEqualTo(0.0);
        assertThat(result.getSeverity()).isEqualTo("UNKNOWN");
        assertThat(result.getAnomalyFeatures()).contains("model_error");
        
        verify(fraudDetectionMLService).detectFraud(transaction);
    }
}