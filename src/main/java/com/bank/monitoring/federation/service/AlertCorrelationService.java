package com.bank.monitoring.federation.service;

import com.bank.monitoring.federation.model.RegionAlert;
import com.bank.monitoring.federation.model.AlertCorrelationResult;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Alert Correlation Service
 * Correlates alerts across multiple regions to identify patterns
 */
@Service
public class AlertCorrelationService {

    private static final Logger logger = LoggerFactory.getLogger(AlertCorrelationService.class);
    
    // Correlation thresholds
    private static final double HIGH_CORRELATION_THRESHOLD = 0.8;
    private static final double MEDIUM_CORRELATION_THRESHOLD = 0.6;
    private static final long TIME_WINDOW_MINUTES = 15;
    
    /**
     * Correlate alerts across regions
     */
    public CompletableFuture<AlertCorrelationResult> correlateAlerts(List<RegionAlert> alerts) {
        logger.info("Correlating {} alerts across regions", alerts.size());
        
        try {
            return CompletableFuture.supplyAsync(() -> {
                if (alerts.isEmpty()) {
                    return createEmptyCorrelationResult();
                }
                
                // Group alerts by type
                Map<String, List<RegionAlert>> alertsByType = alerts.stream()
                    .collect(Collectors.groupingBy(RegionAlert::getAlertType));
                
                // Find the most significant correlation
                AlertCorrelationResult bestCorrelation = null;
                double bestScore = 0.0;
                
                for (Map.Entry<String, List<RegionAlert>> entry : alertsByType.entrySet()) {
                    String alertType = entry.getKey();
                    List<RegionAlert> typeAlerts = entry.getValue();
                    
                    if (typeAlerts.size() > 1) {
                        AlertCorrelationResult correlation = analyzeAlertCorrelation(alertType, typeAlerts);
                        
                        if (correlation.getCorrelationScore() > bestScore) {
                            bestScore = correlation.getCorrelationScore();
                            bestCorrelation = correlation;
                        }
                    }
                }
                
                return bestCorrelation != null ? bestCorrelation : createEmptyCorrelationResult();
            });
            
        } catch (Exception e) {
            logger.error("Error correlating alerts: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Analyze correlation between alerts of the same type
     */
    private AlertCorrelationResult analyzeAlertCorrelation(String alertType, List<RegionAlert> alerts) {
        logger.debug("Analyzing correlation for alert type: {} with {} alerts", alertType, alerts.size());
        
        // Sort alerts by timestamp
        List<RegionAlert> sortedAlerts = alerts.stream()
            .sorted(Comparator.comparing(RegionAlert::getTimestamp))
            .collect(Collectors.toList());
        
        // Calculate time-based correlation
        double timeCorrelation = calculateTimeCorrelation(sortedAlerts);
        
        // Calculate severity-based correlation
        double severityCorrelation = calculateSeverityCorrelation(sortedAlerts);
        
        // Calculate metric-based correlation
        double metricCorrelation = calculateMetricCorrelation(sortedAlerts);
        
        // Combined correlation score
        double overallScore = (timeCorrelation * 0.4 + severityCorrelation * 0.3 + metricCorrelation * 0.3);
        
        List<String> affectedRegions = sortedAlerts.stream()
            .map(RegionAlert::getRegion)
            .distinct()
            .collect(Collectors.toList());
        
        Map<String, Object> correlationMetrics = Map.of(
            "correlation_type", determineCorrelationType(overallScore, affectedRegions.size()),
            "affected_regions", affectedRegions.size(),
            "time_correlation", timeCorrelation,
            "severity_correlation", severityCorrelation,
            "metric_correlation", metricCorrelation,
            "pattern_confidence", overallScore,
            "time_window_minutes", calculateTimeWindow(sortedAlerts),
            "potential_cause", determinePotentialCause(alertType, overallScore, affectedRegions.size())
        );
        
        return new AlertCorrelationResult(
            generateCorrelationId(),
            LocalDateTime.now(),
            affectedRegions,
            alertType,
            overallScore,
            correlationMetrics
        );
    }
    
    /**
     * Calculate time-based correlation
     */
    private double calculateTimeCorrelation(List<RegionAlert> alerts) {
        if (alerts.size() < 2) return 0.0;
        
        long maxTimeDiff = 0;
        for (int i = 1; i < alerts.size(); i++) {
            long timeDiff = ChronoUnit.MINUTES.between(alerts.get(0).getTimestamp(), alerts.get(i).getTimestamp());
            maxTimeDiff = Math.max(maxTimeDiff, timeDiff);
        }
        
        // Higher correlation for alerts occurring within shorter time windows
        if (maxTimeDiff <= 5) return 0.9;
        if (maxTimeDiff <= 10) return 0.7;
        if (maxTimeDiff <= TIME_WINDOW_MINUTES) return 0.5;
        return 0.2;
    }
    
    /**
     * Calculate severity-based correlation
     */
    private double calculateSeverityCorrelation(List<RegionAlert> alerts) {
        if (alerts.size() < 2) return 0.0;
        
        Set<String> severities = alerts.stream()
            .map(RegionAlert::getSeverity)
            .collect(Collectors.toSet());
        
        // Higher correlation for alerts with same severity
        if (severities.size() == 1) return 0.9;
        if (severities.size() == 2) return 0.6;
        return 0.3;
    }
    
    /**
     * Calculate metric-based correlation
     */
    private double calculateMetricCorrelation(List<RegionAlert> alerts) {
        if (alerts.size() < 2) return 0.0;
        
        // Analyze similar metric patterns
        List<Double> metricValues = alerts.stream()
            .map(alert -> extractPrimaryMetricValue(alert))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (metricValues.size() < 2) return 0.5;
        
        // Calculate coefficient of variation
        double mean = metricValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = metricValues.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .average()
            .orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        double cv = mean > 0 ? stdDev / mean : 1.0;
        
        // Lower coefficient of variation indicates higher correlation
        return Math.max(0.0, 1.0 - cv);
    }
    
    /**
     * Extract primary metric value from alert
     */
    private Double extractPrimaryMetricValue(RegionAlert alert) {
        Map<String, Object> metrics = alert.getAlertMetrics();
        
        // Try to extract the main metric value based on alert type
        String alertType = alert.getAlertType();
        
        if (alertType.contains("CPU")) {
            return (Double) metrics.get("cpu_usage");
        } else if (alertType.contains("MEMORY")) {
            return (Double) metrics.get("memory_usage");
        } else if (alertType.contains("RESPONSE")) {
            return (Double) metrics.get("response_time");
        }
        
        // Return first numeric value found
        return metrics.values().stream()
            .filter(value -> value instanceof Number)
            .map(value -> ((Number) value).doubleValue())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Determine correlation type
     */
    private String determineCorrelationType(double score, int affectedRegions) {
        if (score >= HIGH_CORRELATION_THRESHOLD && affectedRegions >= 2) {
            return "CROSS_REGION_PATTERN";
        } else if (score >= MEDIUM_CORRELATION_THRESHOLD) {
            return "REGIONAL_CORRELATION";
        } else {
            return "ISOLATED_INCIDENTS";
        }
    }
    
    /**
     * Calculate time window for alerts
     */
    private long calculateTimeWindow(List<RegionAlert> alerts) {
        if (alerts.size() < 2) return 0;
        
        LocalDateTime first = alerts.get(0).getTimestamp();
        LocalDateTime last = alerts.get(alerts.size() - 1).getTimestamp();
        
        return ChronoUnit.MINUTES.between(first, last);
    }
    
    /**
     * Determine potential cause
     */
    private String determinePotentialCause(String alertType, double score, int affectedRegions) {
        if (score >= HIGH_CORRELATION_THRESHOLD && affectedRegions >= 2) {
            return switch (alertType) {
                case "HIGH_CPU_USAGE" -> "Global traffic spike";
                case "HIGH_MEMORY_USAGE" -> "Memory leak or resource exhaustion";
                case "HIGH_RESPONSE_TIME" -> "Network latency or database slowdown";
                case "HIGH_ERROR_RATE" -> "Upstream service failure";
                default -> "Systematic issue across regions";
            };
        } else if (score >= MEDIUM_CORRELATION_THRESHOLD) {
            return "Regional infrastructure issue";
        } else {
            return "Isolated regional incident";
        }
    }
    
    /**
     * Create empty correlation result
     */
    private AlertCorrelationResult createEmptyCorrelationResult() {
        return new AlertCorrelationResult(
            generateCorrelationId(),
            LocalDateTime.now(),
            List.of(),
            "NO_ALERTS",
            0.0,
            Map.of(
                "correlation_type", "NO_CORRELATION",
                "affected_regions", 0,
                "pattern_confidence", 0.0
            )
        );
    }
    
    /**
     * Generate unique correlation ID
     */
    private String generateCorrelationId() {
        return "CORRELATION_" + System.currentTimeMillis();
    }
}