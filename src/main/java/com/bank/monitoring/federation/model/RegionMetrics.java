package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Region Metrics
 * Represents performance and health metrics from a specific region
 */
public class RegionMetrics {
    
    private final String region;
    private final LocalDateTime timestamp;
    private final Map<String, Double> systemMetrics;
    private final String regionStatus;
    private final Map<String, Double> bankingMetrics;
    
    public RegionMetrics(String region, LocalDateTime timestamp, Map<String, Double> systemMetrics,
                        String regionStatus, Map<String, Double> bankingMetrics) {
        this.region = region;
        this.timestamp = timestamp;
        this.systemMetrics = systemMetrics;
        this.regionStatus = regionStatus;
        this.bankingMetrics = bankingMetrics;
    }
    
    // Getters
    public String getRegion() { return region; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Double> getSystemMetrics() { return systemMetrics; }
    public String getRegionStatus() { return regionStatus; }
    public Map<String, Double> getBankingMetrics() { return bankingMetrics; }
    
    // Computed metrics
    public boolean isHealthy() {
        return "HEALTHY".equals(regionStatus);
    }
    
    public double getCpuUsage() {
        return systemMetrics.getOrDefault("cpu_usage", 0.0);
    }
    
    public double getMemoryUsage() {
        return systemMetrics.getOrDefault("memory_usage", 0.0);
    }
    
    public double getResponseTime() {
        return systemMetrics.getOrDefault("response_time", 0.0);
    }
    
    public double getTransactionVolume() {
        return bankingMetrics.getOrDefault("banking_transactions", 0.0);
    }
}