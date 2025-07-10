package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * System Metrics
 * Represents system performance metrics for anomaly detection
 */
public class SystemMetrics {
    
    private final LocalDateTime timestamp;
    private final double cpuUsage;
    private final double memoryUsage;
    private final double diskUsage;
    private final long responseTime;
    private final int requestRate;
    private final int errorRate;
    private final Map<String, Double> additionalMetrics;
    
    public SystemMetrics(LocalDateTime timestamp, double cpuUsage, double memoryUsage,
                        double diskUsage, long responseTime, int requestRate,
                        int errorRate, Map<String, Double> additionalMetrics) {
        this.timestamp = timestamp;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.diskUsage = diskUsage;
        this.responseTime = responseTime;
        this.requestRate = requestRate;
        this.errorRate = errorRate;
        this.additionalMetrics = additionalMetrics;
    }
    
    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getCpuUsage() { return cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }
    public double getDiskUsage() { return diskUsage; }
    public long getResponseTime() { return responseTime; }
    public int getRequestRate() { return requestRate; }
    public int getErrorRate() { return errorRate; }
    public Map<String, Double> getAdditionalMetrics() { return additionalMetrics; }
}