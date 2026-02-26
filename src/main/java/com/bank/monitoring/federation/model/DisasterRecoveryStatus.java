package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Disaster Recovery Status
 * Represents the disaster recovery status across regions
 */
public class DisasterRecoveryStatus {
    
    private final String statusId;
    private final LocalDateTime checkTime;
    private final List<String> monitoredRegions;
    private final String overallStatus;
    private final Map<String, Object> statusDetails;
    private final List<String> issues;
    
    public DisasterRecoveryStatus(String statusId, LocalDateTime checkTime,
                                 List<String> monitoredRegions, String overallStatus,
                                 Map<String, Object> statusDetails, List<String> issues) {
        this.statusId = statusId;
        this.checkTime = checkTime;
        this.monitoredRegions = monitoredRegions;
        this.overallStatus = overallStatus;
        this.statusDetails = statusDetails;
        this.issues = issues;
    }
    
    // Getters
    public String getStatusId() { return statusId; }
    public LocalDateTime getCheckTime() { return checkTime; }
    public List<String> getMonitoredRegions() { return monitoredRegions; }
    public String getOverallStatus() { return overallStatus; }
    public Map<String, Object> getStatusDetails() { return statusDetails; }
    public List<String> getIssues() { return issues; }
    
    // Computed properties
    public boolean isHealthy() {
        return "HEALTHY".equals(overallStatus);
    }
    
    public boolean isFailoverReady() {
        return (Boolean) statusDetails.getOrDefault("failover_ready", false);
    }
    
    public String getPrimaryRegion() {
        return (String) statusDetails.getOrDefault("primary_region", "unknown");
    }
    
    public double getReplicationLag() {
        return (Double) statusDetails.getOrDefault("replication_lag", 0.0);
    }
    
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
}