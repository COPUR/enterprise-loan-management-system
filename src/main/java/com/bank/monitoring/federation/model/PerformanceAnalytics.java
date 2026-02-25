package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Performance Analytics
 * Represents performance analytics across regions
 */
public class PerformanceAnalytics {
    
    private final String analyticsId;
    private final LocalDateTime analysisTime;
    private final List<String> regions;
    private final Map<String, Double> globalMetrics;
    private final Map<String, Map<String, Double>> regionPerformance;
    private final List<String> insights;
    
    public PerformanceAnalytics(String analyticsId, LocalDateTime analysisTime,
                               List<String> regions, Map<String, Double> globalMetrics,
                               Map<String, Map<String, Double>> regionPerformance,
                               List<String> insights) {
        this.analyticsId = analyticsId;
        this.analysisTime = analysisTime;
        this.regions = regions;
        this.globalMetrics = globalMetrics;
        this.regionPerformance = regionPerformance;
        this.insights = insights;
    }
    
    // Getters
    public String getAnalyticsId() { return analyticsId; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public List<String> getRegions() { return regions; }
    public Map<String, Double> getGlobalMetrics() { return globalMetrics; }
    public Map<String, Map<String, Double>> getRegionPerformance() { return regionPerformance; }
    public List<String> getInsights() { return insights; }
    
    // Computed properties
    public double getGlobalThroughput() {
        return globalMetrics.getOrDefault("global_throughput", 0.0);
    }
    
    public double getGlobalLatencyP95() {
        return globalMetrics.getOrDefault("global_latency_p95", 0.0);
    }
    
    public double getGlobalErrorRate() {
        return globalMetrics.getOrDefault("global_error_rate", 0.0);
    }
    
    public double getGlobalAvailability() {
        return globalMetrics.getOrDefault("global_availability", 0.0);
    }
    
    public boolean hasPerformanceIssues() {
        return getGlobalErrorRate() > 0.05 || getGlobalLatencyP95() > 200.0;
    }
    
    public String getBestPerformingRegion() {
        return regionPerformance.entrySet().stream()
            .min((e1, e2) -> Double.compare(
                e1.getValue().get("latency_p95"), 
                e2.getValue().get("latency_p95")
            ))
            .map(Map.Entry::getKey)
            .orElse("unknown");
    }
}