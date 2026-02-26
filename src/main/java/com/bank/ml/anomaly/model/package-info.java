/**
 * ML Anomaly Detection Model Classes
 * 
 * This package contains all the model classes used for machine learning-based anomaly detection
 * in the enterprise banking system. These models represent various types of data and results
 * used throughout the anomaly detection pipeline.
 * 
 * Key Model Categories:
 * 
 * 1. Input Data Models:
 *    - TransactionData: Represents transaction information for fraud detection
 *    - SystemMetrics: Contains system performance metrics
 *    - HistoricalData: Historical patterns and trends data
 *    - TrainingData: Data used for training ML models
 * 
 * 2. Analysis Result Models:
 *    - FraudDetectionResult: Results from fraud detection analysis
 *    - TransactionPatternAnalysis: Transaction pattern analysis results
 *    - SystemPerformanceAnomaly: System performance anomaly detection results
 *    - IncidentPrediction: Incident prediction results
 *    - AnomalyDetectionResult: General anomaly detection results
 * 
 * 3. ML Model Management:
 *    - MLModel: Represents a trained ML model
 *    - MLModelTrainingResult: Results from model training
 *    - ModelPerformanceMetrics: Model performance tracking
 *    - ModelRetrainingResult: Results from model retraining
 * 
 * 4. Reporting Models:
 *    - AnomalyReport: Comprehensive anomaly reporting
 *    - AnomalyStatistics: Statistical analysis of anomalies
 * 
 * 5. Exception Models:
 *    - MLModelException: Custom exception for ML-related errors
 * 
 * All models are designed to be immutable where possible and include proper validation
 * and serialization support for integration with the banking system's monitoring
 * and alerting infrastructure.
 * 
 * @author Enterprise Banking Team
 * @version 1.0
 * @since 2024-01-01
 */
package com.bank.ml.anomaly.model;