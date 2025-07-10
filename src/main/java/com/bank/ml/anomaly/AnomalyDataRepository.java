package com.bank.ml.anomaly;

import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bank.ml.anomaly.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Anomaly Data Repository
 * Manages storage and retrieval of anomaly detection results
 */
@Repository
public class AnomalyDataRepository {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDataRepository.class);
    
    // In-memory storage for anomaly data (in production, this would be persistent storage)
    private final Map<String, AnomalyDetectionResult> anomalies = new ConcurrentHashMap<>();
    private final Map<String, List<String>> customerAnomalyIndex = new ConcurrentHashMap<>();
    
    /**
     * Save anomaly detection result
     */
    public AnomalyDetectionResult save(AnomalyDetectionResult anomalyResult) {
        logger.debug("Saving anomaly detection result: {}", anomalyResult.getAnomalyId());
        
        try {
            // Validate anomaly result
            validateAnomalyResult(anomalyResult);
            
            // Store anomaly
            anomalies.put(anomalyResult.getAnomalyId(), anomalyResult);
            
            // Update customer index
            updateCustomerIndex(anomalyResult);
            
            logger.info("Anomaly detection result saved successfully: {} for customer {}", 
                anomalyResult.getAnomalyId(), anomalyResult.getCustomerId());
            
            return anomalyResult;
            
        } catch (Exception e) {
            logger.error("Error saving anomaly result {}: {}", 
                anomalyResult.getAnomalyId(), e.getMessage());
            throw new RuntimeException("Failed to save anomaly result", e);
        }
    }
    
    /**
     * Find anomalies by customer ID
     */
    public List<AnomalyDetectionResult> findByCustomerId(String customerId) {
        logger.debug("Finding anomalies for customer: {}", customerId);
        
        try {
            List<String> anomalyIds = customerAnomalyIndex.get(customerId);
            
            if (anomalyIds == null || anomalyIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            return anomalyIds.stream()
                .map(anomalies::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AnomalyDetectionResult::getDetectionTime).reversed())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding anomalies for customer {}: {}", customerId, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find anomalies by date range
     */
    public List<AnomalyDetectionResult> findByDateRange(LocalDateTime startTime, LocalDateTime endTime) {
        logger.debug("Finding anomalies for date range: {} to {}", startTime, endTime);
        
        try {
            return anomalies.values().stream()
                .filter(anomaly -> {
                    LocalDateTime detectionTime = anomaly.getDetectionTime();
                    return !detectionTime.isBefore(startTime) && !detectionTime.isAfter(endTime);
                })
                .sorted(Comparator.comparing(AnomalyDetectionResult::getDetectionTime).reversed())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding anomalies for date range: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find anomalies by type
     */
    public List<AnomalyDetectionResult> findByAnomalyType(String anomalyType) {
        logger.debug("Finding anomalies by type: {}", anomalyType);
        
        try {
            return anomalies.values().stream()
                .filter(anomaly -> anomalyType.equals(anomaly.getAnomalyType()))
                .sorted(Comparator.comparing(AnomalyDetectionResult::getDetectionTime).reversed())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding anomalies by type {}: {}", anomalyType, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find anomalies by severity
     */
    public List<AnomalyDetectionResult> findBySeverity(String severity) {
        logger.debug("Finding anomalies by severity: {}", severity);
        
        try {
            return anomalies.values().stream()
                .filter(anomaly -> severity.equals(anomaly.getSeverity()))
                .sorted(Comparator.comparing(AnomalyDetectionResult::getDetectionTime).reversed())
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding anomalies by severity {}: {}", severity, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Find anomalies by customer and type
     */
    public List<AnomalyDetectionResult> findByCustomerIdAndType(String customerId, String anomalyType) {
        logger.debug("Finding anomalies for customer {} and type {}", customerId, anomalyType);
        
        try {
            return findByCustomerId(customerId).stream()
                .filter(anomaly -> anomalyType.equals(anomaly.getAnomalyType()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error finding anomalies for customer {} and type {}: {}", 
                customerId, anomalyType, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get anomaly statistics
     */
    public AnomalyStatistics getStatistics() {
        logger.debug("Calculating anomaly statistics");
        
        try {
            List<AnomalyDetectionResult> allAnomalies = new ArrayList<>(anomalies.values());
            
            if (allAnomalies.isEmpty()) {
                return new AnomalyStatistics(0, 0, 0, 0, 0.0, Map.of(), Map.of());
            }
            
            int totalAnomalies = allAnomalies.size();
            
            // Count by severity
            int highSeverity = (int) allAnomalies.stream()
                .filter(a -> "HIGH".equals(a.getSeverity()))
                .count();
            int mediumSeverity = (int) allAnomalies.stream()
                .filter(a -> "MEDIUM".equals(a.getSeverity()))
                .count();
            int lowSeverity = (int) allAnomalies.stream()
                .filter(a -> "LOW".equals(a.getSeverity()))
                .count();
            
            // Calculate average confidence
            double averageConfidence = allAnomalies.stream()
                .mapToDouble(AnomalyDetectionResult::getConfidenceScore)
                .average()
                .orElse(0.0);
            
            // Count by type
            Map<String, Long> typeDistribution = allAnomalies.stream()
                .collect(Collectors.groupingBy(
                    AnomalyDetectionResult::getAnomalyType,
                    Collectors.counting()
                ));
            
            // Count by customer
            Map<String, Long> customerDistribution = allAnomalies.stream()
                .collect(Collectors.groupingBy(
                    AnomalyDetectionResult::getCustomerId,
                    Collectors.counting()
                ));
            
            return new AnomalyStatistics(
                totalAnomalies,
                highSeverity,
                mediumSeverity,
                lowSeverity,
                averageConfidence,
                typeDistribution,
                customerDistribution
            );
            
        } catch (Exception e) {
            logger.error("Error calculating anomaly statistics: {}", e.getMessage());
            return new AnomalyStatistics(0, 0, 0, 0, 0.0, Map.of(), Map.of());
        }
    }
    
    /**
     * Get recent anomalies (last 24 hours)
     */
    public List<AnomalyDetectionResult> getRecentAnomalies() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        return findByDateRange(yesterday, LocalDateTime.now());
    }
    
    /**
     * Get high severity anomalies
     */
    public List<AnomalyDetectionResult> getHighSeverityAnomalies() {
        return findBySeverity("HIGH");
    }
    
    /**
     * Delete anomaly by ID
     */
    public void deleteById(String anomalyId) {
        logger.debug("Deleting anomaly: {}", anomalyId);
        
        try {
            AnomalyDetectionResult removed = anomalies.remove(anomalyId);
            
            if (removed != null) {
                // Update customer index
                String customerId = removed.getCustomerId();
                List<String> customerAnomalies = customerAnomalyIndex.get(customerId);
                if (customerAnomalies != null) {
                    customerAnomalies.remove(anomalyId);
                    if (customerAnomalies.isEmpty()) {
                        customerAnomalyIndex.remove(customerId);
                    }
                }
                
                logger.info("Anomaly {} deleted successfully", anomalyId);
            }
            
        } catch (Exception e) {
            logger.error("Error deleting anomaly {}: {}", anomalyId, e.getMessage());
            throw new RuntimeException("Failed to delete anomaly", e);
        }
    }
    
    /**
     * Get anomaly by ID
     */
    public Optional<AnomalyDetectionResult> findById(String anomalyId) {
        logger.debug("Finding anomaly by ID: {}", anomalyId);
        
        try {
            return Optional.ofNullable(anomalies.get(anomalyId));
        } catch (Exception e) {
            logger.error("Error finding anomaly by ID {}: {}", anomalyId, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Get all anomalies
     */
    public List<AnomalyDetectionResult> findAll() {
        logger.debug("Finding all anomalies");
        
        try {
            return new ArrayList<>(anomalies.values());
        } catch (Exception e) {
            logger.error("Error finding all anomalies: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Count anomalies by customer
     */
    public long countByCustomerId(String customerId) {
        return findByCustomerId(customerId).size();
    }
    
    /**
     * Count total anomalies
     */
    public long count() {
        return anomalies.size();
    }
    
    /**
     * Validate anomaly result
     */
    private void validateAnomalyResult(AnomalyDetectionResult anomalyResult) {
        if (anomalyResult.getAnomalyId() == null || anomalyResult.getAnomalyId().isEmpty()) {
            throw new IllegalArgumentException("Anomaly ID cannot be null or empty");
        }
        
        if (anomalyResult.getCustomerId() == null || anomalyResult.getCustomerId().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (anomalyResult.getAnomalyType() == null || anomalyResult.getAnomalyType().isEmpty()) {
            throw new IllegalArgumentException("Anomaly type cannot be null or empty");
        }
        
        if (anomalyResult.getDetectionTime() == null) {
            throw new IllegalArgumentException("Detection time cannot be null");
        }
        
        if (anomalyResult.getConfidenceScore() < 0.0 || anomalyResult.getConfidenceScore() > 1.0) {
            throw new IllegalArgumentException("Confidence score must be between 0.0 and 1.0");
        }
    }
    
    /**
     * Update customer index
     */
    private void updateCustomerIndex(AnomalyDetectionResult anomalyResult) {
        String customerId = anomalyResult.getCustomerId();
        String anomalyId = anomalyResult.getAnomalyId();
        
        customerAnomalyIndex.computeIfAbsent(customerId, k -> new ArrayList<>()).add(anomalyId);
    }
}