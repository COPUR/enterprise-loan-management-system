package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Region Failover Result
 * Represents the result of a region failover operation
 */
public class RegionFailoverResult {
    
    private final String failoverId;
    private final LocalDateTime failoverTime;
    private final String failedRegion;
    private final List<String> healthyRegions;
    private final String failoverStatus;
    private final Map<String, Object> failoverMetrics;
    
    public RegionFailoverResult(String failoverId, LocalDateTime failoverTime,
                               String failedRegion, List<String> healthyRegions,
                               String failoverStatus, Map<String, Object> failoverMetrics) {
        this.failoverId = failoverId;
        this.failoverTime = failoverTime;
        this.failedRegion = failedRegion;
        this.healthyRegions = healthyRegions;
        this.failoverStatus = failoverStatus;
        this.failoverMetrics = failoverMetrics;
    }
    
    // Getters
    public String getFailoverId() { return failoverId; }
    public LocalDateTime getFailoverTime() { return failoverTime; }
    public String getFailedRegion() { return failedRegion; }
    public List<String> getHealthyRegions() { return healthyRegions; }
    public String getFailoverStatus() { return failoverStatus; }
    public Map<String, Object> getFailoverMetrics() { return failoverMetrics; }
    
    // Computed properties
    public boolean isCompleted() {
        return "COMPLETED".equals(failoverStatus);
    }
    
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(failoverStatus);
    }
    
    public boolean isFailed() {
        return "FAILED".equals(failoverStatus);
    }
    
    public double getFailoverDuration() {
        return (Double) failoverMetrics.getOrDefault("failover_duration", 0.0);
    }
    
    public double getTrafficRedirected() {
        return (Double) failoverMetrics.getOrDefault("traffic_redirected", 0.0);
    }
    
    public String getNewPrimaryRegion() {
        return (String) failoverMetrics.getOrDefault("new_primary", "unknown");
    }
    
    public String getDataSynchronizationStatus() {
        return (String) failoverMetrics.getOrDefault("data_synchronization", "unknown");
    }
}