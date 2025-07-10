package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Federation Status
 * Represents the overall status of the cross-region monitoring federation
 */
public class FederationStatus {
    
    private final String federationId;
    private final LocalDateTime statusTime;
    private final List<String> regions;
    private final Map<String, RegionMetrics> regionMetrics;
    private final DisasterRecoveryStatus disasterRecoveryStatus;
    private final ComplianceStatus complianceStatus;
    private final PerformanceAnalytics performanceAnalytics;
    private final String overallHealth;
    
    public FederationStatus(String federationId, LocalDateTime statusTime,
                           List<String> regions, Map<String, RegionMetrics> regionMetrics,
                           DisasterRecoveryStatus disasterRecoveryStatus,
                           ComplianceStatus complianceStatus,
                           PerformanceAnalytics performanceAnalytics,
                           String overallHealth) {
        this.federationId = federationId;
        this.statusTime = statusTime;
        this.regions = regions;
        this.regionMetrics = regionMetrics;
        this.disasterRecoveryStatus = disasterRecoveryStatus;
        this.complianceStatus = complianceStatus;
        this.performanceAnalytics = performanceAnalytics;
        this.overallHealth = overallHealth;
    }
    
    // Getters
    public String getFederationId() { return federationId; }
    public LocalDateTime getStatusTime() { return statusTime; }
    public List<String> getRegions() { return regions; }
    public Map<String, RegionMetrics> getRegionMetrics() { return regionMetrics; }
    public DisasterRecoveryStatus getDisasterRecoveryStatus() { return disasterRecoveryStatus; }
    public ComplianceStatus getComplianceStatus() { return complianceStatus; }
    public PerformanceAnalytics getPerformanceAnalytics() { return performanceAnalytics; }
    public String getOverallHealth() { return overallHealth; }
    
    // Computed properties
    public boolean isHealthy() {
        return "EXCELLENT".equals(overallHealth) || "GOOD".equals(overallHealth);
    }
    
    public boolean isCritical() {
        return "CRITICAL".equals(overallHealth);
    }
    
    public boolean isDegraded() {
        return "DEGRADED".equals(overallHealth);
    }
    
    public int getHealthyRegions() {
        return (int) regionMetrics.values().stream()
            .filter(RegionMetrics::isHealthy)
            .count();
    }
    
    public int getTotalRegions() {
        return regions.size();
    }
    
    public double getHealthPercentage() {
        if (getTotalRegions() == 0) return 0.0;
        return (double) getHealthyRegions() / getTotalRegions() * 100.0;
    }
    
    public double getTotalGlobalThroughput() {
        return performanceAnalytics.getGlobalThroughput();
    }
    
    public boolean isDisasterRecoveryReady() {
        return disasterRecoveryStatus.isHealthy() && disasterRecoveryStatus.isFailoverReady();
    }
    
    public boolean isCompliant() {
        return complianceStatus.isFullyCompliant();
    }
}