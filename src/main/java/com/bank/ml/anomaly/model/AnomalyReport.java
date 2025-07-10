package com.bank.ml.anomaly.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Anomaly Report
 * Comprehensive report of anomaly detection results
 */
public class AnomalyReport {
    
    private final String reportId;
    private final LocalDateTime reportStartTime;
    private final LocalDateTime reportEndTime;
    private final LocalDateTime generationTime;
    private final List<AnomalyDetectionResult> anomalies;
    private final AnomalyStatistics statistics;
    private final Map<String, Object> reportMetrics;
    private final String reportStatus;
    
    public AnomalyReport(String reportId, LocalDateTime reportStartTime, LocalDateTime reportEndTime,
                        LocalDateTime generationTime, List<AnomalyDetectionResult> anomalies,
                        AnomalyStatistics statistics, Map<String, Object> reportMetrics,
                        String reportStatus) {
        this.reportId = reportId;
        this.reportStartTime = reportStartTime;
        this.reportEndTime = reportEndTime;
        this.generationTime = generationTime;
        this.anomalies = anomalies;
        this.statistics = statistics;
        this.reportMetrics = reportMetrics;
        this.reportStatus = reportStatus;
    }
    
    // Getters
    public String getReportId() { return reportId; }
    public LocalDateTime getReportStartTime() { return reportStartTime; }
    public LocalDateTime getReportEndTime() { return reportEndTime; }
    public LocalDateTime getGenerationTime() { return generationTime; }
    public List<AnomalyDetectionResult> getAnomalies() { return anomalies; }
    public AnomalyStatistics getStatistics() { return statistics; }
    public Map<String, Object> getReportMetrics() { return reportMetrics; }
    public String getReportStatus() { return reportStatus; }
    
    // Computed metrics
    public int getTotalAnomalies() {
        return anomalies.size();
    }
    
    public int getHighSeverityAnomalies() {
        return (int) anomalies.stream()
            .filter(a -> "HIGH".equals(a.getSeverity()))
            .count();
    }
}