package com.bank.monitoring.federation.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Region Alert
 * Represents an alert from a specific region
 */
public class RegionAlert {
    
    private final String alertId;
    private final String region;
    private final String alertType;
    private final String severity;
    private final LocalDateTime timestamp;
    private final Map<String, Object> alertMetrics;
    private final String description;
    
    public RegionAlert(String alertId, String region, String alertType, String severity,
                      LocalDateTime timestamp, Map<String, Object> alertMetrics, String description) {
        this.alertId = alertId;
        this.region = region;
        this.alertType = alertType;
        this.severity = severity;
        this.timestamp = timestamp;
        this.alertMetrics = alertMetrics;
        this.description = description;
    }
    
    // Getters
    public String getAlertId() { return alertId; }
    public String getRegion() { return region; }
    public String getAlertType() { return alertType; }
    public String getSeverity() { return severity; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Map<String, Object> getAlertMetrics() { return alertMetrics; }
    public String getDescription() { return description; }
    
    // Computed properties
    public boolean isHighSeverity() {
        return "HIGH".equals(severity);
    }
    
    public boolean isCritical() {
        return "CRITICAL".equals(severity);
    }
}