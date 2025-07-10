package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Global Dashboard Data
 * Represents unified dashboard data across all regions
 */
public class GlobalDashboardData {
    
    private final String dashboardId;
    private final LocalDateTime generationTime;
    private final List<String> regions;
    private final Map<String, Double> globalMetrics;
    private final Map<String, Map<String, Object>> regionSummaries;
    private final List<String> alerts;
    
    public GlobalDashboardData(String dashboardId, LocalDateTime generationTime,
                              List<String> regions, Map<String, Double> globalMetrics,
                              Map<String, Map<String, Object>> regionSummaries,
                              List<String> alerts) {
        this.dashboardId = dashboardId;
        this.generationTime = generationTime;
        this.regions = regions;
        this.globalMetrics = globalMetrics;
        this.regionSummaries = regionSummaries;
        this.alerts = alerts;
    }
    
    // Getters
    public String getDashboardId() { return dashboardId; }
    public LocalDateTime getGenerationTime() { return generationTime; }
    public List<String> getRegions() { return regions; }
    public Map<String, Double> getGlobalMetrics() { return globalMetrics; }
    public Map<String, Map<String, Object>> getRegionSummaries() { return regionSummaries; }
    public List<String> getAlerts() { return alerts; }
    
    // Computed properties
    public double getTotalTransactions() {
        return globalMetrics.getOrDefault("total_transactions", 0.0);
    }
    
    public double getGlobalAverageResponseTime() {
        return globalMetrics.getOrDefault("global_avg_response_time", 0.0);
    }
    
    public double getGlobalCpuUsage() {
        return globalMetrics.getOrDefault("global_avg_cpu_usage", 0.0);
    }
    
    public boolean hasActiveAlerts() {
        return !alerts.isEmpty();
    }
    
    public int getActiveRegions() {
        return (int) regionSummaries.values().stream()
            .filter(summary -> "HEALTHY".equals(summary.get("status")))
            .count();
    }
}