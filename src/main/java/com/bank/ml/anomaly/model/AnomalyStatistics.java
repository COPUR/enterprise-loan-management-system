package com.bank.ml.anomaly.model;

import java.util.Map;

/**
 * Anomaly Statistics
 * Provides statistical analysis of anomaly detection results
 */
public class AnomalyStatistics {
    
    private final int totalAnomalies;
    private final int highSeverityCount;
    private final int mediumSeverityCount;
    private final int lowSeverityCount;
    private final double averageConfidence;
    private final Map<String, Long> typeDistribution;
    private final Map<String, Long> customerDistribution;
    
    public AnomalyStatistics(int totalAnomalies, int highSeverityCount, int mediumSeverityCount,
                            int lowSeverityCount, double averageConfidence,
                            Map<String, Long> typeDistribution, Map<String, Long> customerDistribution) {
        this.totalAnomalies = totalAnomalies;
        this.highSeverityCount = highSeverityCount;
        this.mediumSeverityCount = mediumSeverityCount;
        this.lowSeverityCount = lowSeverityCount;
        this.averageConfidence = averageConfidence;
        this.typeDistribution = typeDistribution;
        this.customerDistribution = customerDistribution;
    }
    
    // Getters
    public int getTotalAnomalies() { return totalAnomalies; }
    public int getHighSeverityCount() { return highSeverityCount; }
    public int getMediumSeverityCount() { return mediumSeverityCount; }
    public int getLowSeverityCount() { return lowSeverityCount; }
    public double getAverageConfidence() { return averageConfidence; }
    public Map<String, Long> getTypeDistribution() { return typeDistribution; }
    public Map<String, Long> getCustomerDistribution() { return customerDistribution; }
    
    // Computed statistics
    public double getHighSeverityPercentage() {
        if (totalAnomalies == 0) return 0.0;
        return (double) highSeverityCount / totalAnomalies * 100.0;
    }
    
    public double getMediumSeverityPercentage() {
        if (totalAnomalies == 0) return 0.0;
        return (double) mediumSeverityCount / totalAnomalies * 100.0;
    }
    
    public double getLowSeverityPercentage() {
        if (totalAnomalies == 0) return 0.0;
        return (double) lowSeverityCount / totalAnomalies * 100.0;
    }
}