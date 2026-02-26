package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Historical Data
 * Represents historical patterns and trends for incident prediction
 */
public class HistoricalData {
    
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<String> services;
    private final Map<String, Object> patterns;
    private final Map<String, Integer> incidentCounts;
    private final Map<String, Double> performanceMetrics;
    private final String dataSource;
    
    public HistoricalData(LocalDateTime startTime, LocalDateTime endTime,
                         List<String> services, Map<String, Object> patterns,
                         Map<String, Integer> incidentCounts, Map<String, Double> performanceMetrics,
                         String dataSource) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.services = services;
        this.patterns = patterns;
        this.incidentCounts = incidentCounts;
        this.performanceMetrics = performanceMetrics;
        this.dataSource = dataSource;
    }
    
    // Getters
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public List<String> getServices() { return services; }
    public Map<String, Object> getPatterns() { return patterns; }
    public Map<String, Integer> getIncidentCounts() { return incidentCounts; }
    public Map<String, Double> getPerformanceMetrics() { return performanceMetrics; }
    public String getDataSource() { return dataSource; }
}