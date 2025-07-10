package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Compliance Status
 * Represents compliance status across regions
 */
public class ComplianceStatus {
    
    private final String complianceId;
    private final LocalDateTime checkTime;
    private final List<String> regions;
    private final Map<String, Map<String, Object>> regionCompliance;
    private final Map<String, Object> globalMetrics;
    
    public ComplianceStatus(String complianceId, LocalDateTime checkTime,
                           List<String> regions, Map<String, Map<String, Object>> regionCompliance,
                           Map<String, Object> globalMetrics) {
        this.complianceId = complianceId;
        this.checkTime = checkTime;
        this.regions = regions;
        this.regionCompliance = regionCompliance;
        this.globalMetrics = globalMetrics;
    }
    
    // Getters
    public String getComplianceId() { return complianceId; }
    public LocalDateTime getCheckTime() { return checkTime; }
    public List<String> getRegions() { return regions; }
    public Map<String, Map<String, Object>> getRegionCompliance() { return regionCompliance; }
    public Map<String, Object> getGlobalMetrics() { return globalMetrics; }
    
    // Computed properties
    public double getGlobalComplianceScore() {
        return (Double) globalMetrics.getOrDefault("global_compliance_score", 0.0);
    }
    
    public int getPendingAudits() {
        return (Integer) globalMetrics.getOrDefault("pending_audits", 0);
    }
    
    public int getComplianceViolations() {
        return (Integer) globalMetrics.getOrDefault("compliance_violations", 0);
    }
    
    public boolean isFullyCompliant() {
        return getComplianceViolations() == 0 && getGlobalComplianceScore() >= 95.0;
    }
    
    public boolean hasViolations() {
        return getComplianceViolations() > 0;
    }
}