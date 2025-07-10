package com.bank.ml.anomaly;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bank.ml.anomaly.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

/**
 * ML-based Real-time Fraud Detection Service
 * Implements advanced machine learning algorithms for fraud detection
 */
@Service
public class FraudDetectionMLService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionMLService.class);
    
    // Fraud detection thresholds
    private static final double HIGH_RISK_THRESHOLD = 0.8;
    private static final double MEDIUM_RISK_THRESHOLD = 0.5;
    private static final double UNUSUAL_AMOUNT_THRESHOLD = 5000.0;
    private static final int NIGHT_HOUR_START = 22;
    private static final int NIGHT_HOUR_END = 6;
    
    /**
     * Detect fraud using ML models for real-time transaction analysis
     */
    public FraudDetectionResult detectFraud(TransactionData transaction) {
        logger.debug("Analyzing transaction {} for fraud detection", transaction.getTransactionId());
        
        try {
            // Extract features for ML model
            Map<String, Double> features = extractFeatures(transaction);
            
            // Calculate risk score using ML model
            double riskScore = calculateRiskScore(features);
            
            // Determine if transaction is anomalous
            boolean isAnomalous = riskScore > MEDIUM_RISK_THRESHOLD;
            
            // Determine severity based on risk score
            String severity = determineSeverity(riskScore);
            
            // Identify specific anomaly features
            List<String> anomalyFeatures = identifyAnomalyFeatures(transaction, features);
            
            // Build metrics
            Map<String, Object> metrics = buildMetrics(riskScore, features);
            
            logger.info("Fraud detection completed for transaction {}: anomalous={}, risk_score={}, severity={}", 
                transaction.getTransactionId(), isAnomalous, riskScore, severity);
            
            return new FraudDetectionResult(
                transaction.getTransactionId(),
                isAnomalous,
                riskScore,
                severity,
                anomalyFeatures,
                metrics
            );
            
        } catch (Exception e) {
            logger.error("Error detecting fraud for transaction {}: {}", 
                transaction.getTransactionId(), e.getMessage());
            throw new MLModelException("Fraud detection failed", e);
        }
    }
    
    /**
     * Extract features from transaction data for ML model
     */
    private Map<String, Double> extractFeatures(TransactionData transaction) {
        Map<String, Double> features = new HashMap<>();
        
        // Amount feature
        features.put("amount", transaction.getAmount());
        features.put("amount_normalized", normalizeAmount(transaction.getAmount()));
        
        // Time features
        features.put("hour_of_day", (double) transaction.getTimestamp().getHour());
        features.put("day_of_week", (double) transaction.getTimestamp().getDayOfWeek().getValue());
        features.put("is_weekend", isWeekend(transaction.getTimestamp()) ? 1.0 : 0.0);
        features.put("is_night_time", isNightTime(transaction.getTimestamp()) ? 1.0 : 0.0);
        
        // Transaction type features
        features.put("is_wire_transfer", "WIRE_TRANSFER".equals(transaction.getTransactionType()) ? 1.0 : 0.0);
        features.put("is_cash_withdrawal", "CASH_WITHDRAWAL".equals(transaction.getTransactionType()) ? 1.0 : 0.0);
        features.put("is_online_payment", "ONLINE_PAYMENT".equals(transaction.getTransactionType()) ? 1.0 : 0.0);
        
        // Contextual features
        Map<String, Object> context = transaction.getContext();
        features.put("is_overseas", context.containsKey("location") && 
            "overseas".equals(context.get("location")) ? 1.0 : 0.0);
        features.put("is_new_device", context.containsKey("device") && 
            "new".equals(context.get("device")) ? 1.0 : 0.0);
        
        return features;
    }
    
    /**
     * Calculate risk score using ML model (simplified implementation)
     */
    private double calculateRiskScore(Map<String, Double> features) {
        // Simplified ML model - in production, this would use a trained model
        double riskScore = 0.0;
        
        // Amount-based risk
        double amount = features.get("amount");
        if (amount > UNUSUAL_AMOUNT_THRESHOLD) {
            riskScore += 0.3;
        }
        if (amount > 10000.0) {
            riskScore += 0.2;
        }
        
        // Time-based risk
        if (features.get("is_night_time") == 1.0) {
            riskScore += 0.2;
        }
        if (features.get("is_weekend") == 1.0) {
            riskScore += 0.1;
        }
        
        // Transaction type risk
        if (features.get("is_wire_transfer") == 1.0) {
            riskScore += 0.15;
        }
        if (features.get("is_cash_withdrawal") == 1.0) {
            riskScore += 0.1;
        }
        
        // Context-based risk
        if (features.get("is_overseas") == 1.0) {
            riskScore += 0.25;
        }
        if (features.get("is_new_device") == 1.0) {
            riskScore += 0.15;
        }
        
        // Normalize risk score
        return Math.min(1.0, riskScore);
    }
    
    /**
     * Determine severity based on risk score
     */
    private String determineSeverity(double riskScore) {
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            return "HIGH";
        } else if (riskScore >= MEDIUM_RISK_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Identify specific anomaly features
     */
    private List<String> identifyAnomalyFeatures(TransactionData transaction, Map<String, Double> features) {
        List<String> anomalyFeatures = new java.util.ArrayList<>();
        
        // Check for unusual amount
        if (transaction.getAmount() > UNUSUAL_AMOUNT_THRESHOLD) {
            anomalyFeatures.add("unusual_amount");
        }
        
        // Check for suspicious timing
        if (features.get("is_night_time") == 1.0) {
            anomalyFeatures.add("suspicious_time");
        }
        
        // Check for overseas transaction
        if (features.get("is_overseas") == 1.0) {
            anomalyFeatures.add("overseas_location");
        }
        
        // Check for new device
        if (features.get("is_new_device") == 1.0) {
            anomalyFeatures.add("new_device");
        }
        
        // Check for high-risk transaction type
        if (features.get("is_wire_transfer") == 1.0) {
            anomalyFeatures.add("wire_transfer");
        }
        
        return anomalyFeatures;
    }
    
    /**
     * Build metrics for the fraud detection result
     */
    private Map<String, Object> buildMetrics(double riskScore, Map<String, Double> features) {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("risk_score", Math.round(riskScore * 100.0));
        metrics.put("confidence", riskScore);
        metrics.put("model_version", "fraud_detection_v2.1");
        metrics.put("feature_count", features.size());
        metrics.put("processing_time", System.currentTimeMillis());
        
        return metrics;
    }
    
    /**
     * Normalize amount for ML model
     */
    private double normalizeAmount(double amount) {
        // Log normalization for amount
        return Math.log(amount + 1.0) / Math.log(10.0);
    }
    
    /**
     * Check if timestamp is on weekend
     */
    private boolean isWeekend(LocalDateTime timestamp) {
        int dayOfWeek = timestamp.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7; // Saturday or Sunday
    }
    
    /**
     * Check if timestamp is during night hours
     */
    private boolean isNightTime(LocalDateTime timestamp) {
        int hour = timestamp.getHour();
        return hour >= NIGHT_HOUR_START || hour <= NIGHT_HOUR_END;
    }
}