package com.bank.ml.anomaly;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bank.ml.anomaly.model.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * System Performance Anomaly Detector
 * Detects performance anomalies in system metrics using ML techniques
 */
@Service
public class SystemPerformanceAnomalyDetector {

    private static final Logger logger = LoggerFactory.getLogger(SystemPerformanceAnomalyDetector.class);
    
    // Performance thresholds
    private static final double CPU_THRESHOLD = 80.0;
    private static final double MEMORY_THRESHOLD = 85.0;
    private static final long RESPONSE_TIME_THRESHOLD = 1000L; // 1 second
    private static final int REQUEST_RATE_THRESHOLD = 200;
    
    // Anomaly detection thresholds
    private static final double ANOMALY_THRESHOLD = 0.7;
    private static final double HIGH_ANOMALY_THRESHOLD = 0.9;
    
    /**
     * Detect system performance anomalies
     */
    public SystemPerformanceAnomaly detectAnomalies(SystemMetrics metrics) {
        logger.debug("Detecting system performance anomalies for timestamp: {}", metrics.getTimestamp());
        
        try {
            // Analyze individual metrics
            double cpuAnomalyScore = analyzeCPUUsage(metrics.getCpuUsage());
            double memoryAnomalyScore = analyzeMemoryUsage(metrics.getMemoryUsage());
            double responseTimeAnomalyScore = analyzeResponseTime(metrics.getResponseTime());
            double requestRateAnomalyScore = analyzeRequestRate(metrics.getRequestRate());
            double customMetricsScore = analyzeCustomMetrics(metrics.getCustomMetrics());
            
            // Calculate overall anomaly score
            double overallScore = calculateOverallAnomalyScore(
                cpuAnomalyScore, memoryAnomalyScore, responseTimeAnomalyScore, 
                requestRateAnomalyScore, customMetricsScore
            );
            
            // Determine if anomalous
            boolean isAnomalous = overallScore > ANOMALY_THRESHOLD;
            
            // Identify anomaly type
            String anomalyType = identifyAnomalyType(
                cpuAnomalyScore, memoryAnomalyScore, responseTimeAnomalyScore, 
                requestRateAnomalyScore, customMetricsScore
            );
            
            // Identify specific anomaly features
            List<String> anomalyFeatures = identifyAnomalyFeatures(
                metrics, cpuAnomalyScore, memoryAnomalyScore, responseTimeAnomalyScore, 
                requestRateAnomalyScore, customMetricsScore
            );
            
            // Build metrics
            Map<String, Object> analysisMetrics = buildAnalysisMetrics(
                cpuAnomalyScore, memoryAnomalyScore, responseTimeAnomalyScore, 
                requestRateAnomalyScore, customMetricsScore, overallScore
            );
            
            String anomalyId = generateAnomalyId(metrics.getTimestamp());
            
            logger.info("Performance anomaly detection completed: anomalous={}, type={}, score={}", 
                isAnomalous, anomalyType, overallScore);
            
            return new SystemPerformanceAnomaly(
                anomalyId,
                metrics.getTimestamp(),
                isAnomalous,
                overallScore,
                anomalyType,
                anomalyFeatures,
                analysisMetrics
            );
            
        } catch (Exception e) {
            logger.error("Error detecting system performance anomalies: {}", e.getMessage());
            throw new MLModelException("Performance anomaly detection failed", e);
        }
    }
    
    /**
     * Analyze CPU usage for anomalies
     */
    private double analyzeCPUUsage(double cpuUsage) {
        // Normalized scoring based on thresholds
        if (cpuUsage > 95.0) {
            return 1.0;
        } else if (cpuUsage > CPU_THRESHOLD) {
            return 0.5 + (cpuUsage - CPU_THRESHOLD) / (95.0 - CPU_THRESHOLD) * 0.5;
        } else if (cpuUsage > 60.0) {
            return (cpuUsage - 60.0) / (CPU_THRESHOLD - 60.0) * 0.5;
        }
        return 0.0;
    }
    
    /**
     * Analyze memory usage for anomalies
     */
    private double analyzeMemoryUsage(double memoryUsage) {
        // Normalized scoring based on thresholds
        if (memoryUsage > 95.0) {
            return 1.0;
        } else if (memoryUsage > MEMORY_THRESHOLD) {
            return 0.6 + (memoryUsage - MEMORY_THRESHOLD) / (95.0 - MEMORY_THRESHOLD) * 0.4;
        } else if (memoryUsage > 70.0) {
            return (memoryUsage - 70.0) / (MEMORY_THRESHOLD - 70.0) * 0.6;
        }
        return 0.0;
    }
    
    /**
     * Analyze response time for anomalies
     */
    private double analyzeResponseTime(long responseTime) {
        // Exponential scoring for response time
        if (responseTime > 5000L) {
            return 1.0;
        } else if (responseTime > RESPONSE_TIME_THRESHOLD) {
            return 0.4 + Math.log(responseTime - RESPONSE_TIME_THRESHOLD + 1) / Math.log(4000) * 0.6;
        } else if (responseTime > 500L) {
            return (responseTime - 500L) / (RESPONSE_TIME_THRESHOLD - 500L) * 0.4;
        }
        return 0.0;
    }
    
    /**
     * Analyze request rate for anomalies
     */
    private double analyzeRequestRate(int requestRate) {
        // High request rate can indicate DDoS or system overload
        if (requestRate > 1000) {
            return 1.0;
        } else if (requestRate > REQUEST_RATE_THRESHOLD) {
            return 0.5 + (requestRate - REQUEST_RATE_THRESHOLD) / (1000.0 - REQUEST_RATE_THRESHOLD) * 0.5;
        } else if (requestRate > 100) {
            return (requestRate - 100) / (REQUEST_RATE_THRESHOLD - 100.0) * 0.5;
        }
        return 0.0;
    }
    
    /**
     * Analyze custom metrics for anomalies
     */
    private double analyzeCustomMetrics(Map<String, Object> customMetrics) {
        if (customMetrics == null || customMetrics.isEmpty()) {
            return 0.0;
        }
        
        double totalAnomalyScore = 0.0;
        int metricsCount = 0;
        
        // Analyze disk usage
        if (customMetrics.containsKey("disk_usage")) {
            double diskUsage = ((Number) customMetrics.get("disk_usage")).doubleValue();
            if (diskUsage > 90.0) {
                totalAnomalyScore += 0.8;
            } else if (diskUsage > 80.0) {
                totalAnomalyScore += 0.4;
            }
            metricsCount++;
        }
        
        // Analyze network latency
        if (customMetrics.containsKey("network_latency")) {
            double networkLatency = ((Number) customMetrics.get("network_latency")).doubleValue();
            if (networkLatency > 500.0) {
                totalAnomalyScore += 0.7;
            } else if (networkLatency > 200.0) {
                totalAnomalyScore += 0.3;
            }
            metricsCount++;
        }
        
        // Analyze database connection pool
        if (customMetrics.containsKey("db_connections")) {
            double dbConnections = ((Number) customMetrics.get("db_connections")).doubleValue();
            if (dbConnections > 90.0) {
                totalAnomalyScore += 0.6;
            } else if (dbConnections > 75.0) {
                totalAnomalyScore += 0.3;
            }
            metricsCount++;
        }
        
        return metricsCount > 0 ? totalAnomalyScore / metricsCount : 0.0;
    }
    
    /**
     * Calculate overall anomaly score
     */
    private double calculateOverallAnomalyScore(double cpuScore, double memoryScore, 
                                               double responseTimeScore, double requestRateScore, 
                                               double customMetricsScore) {
        
        // Weighted combination of scores
        double weightedScore = 
            cpuScore * 0.25 +
            memoryScore * 0.25 +
            responseTimeScore * 0.3 +
            requestRateScore * 0.15 +
            customMetricsScore * 0.05;
        
        return Math.min(1.0, weightedScore);
    }
    
    /**
     * Identify primary anomaly type
     */
    private String identifyAnomalyType(double cpuScore, double memoryScore, 
                                     double responseTimeScore, double requestRateScore, 
                                     double customMetricsScore) {
        
        double maxScore = Math.max(Math.max(cpuScore, memoryScore), 
                                  Math.max(responseTimeScore, requestRateScore));
        maxScore = Math.max(maxScore, customMetricsScore);
        
        if (maxScore < ANOMALY_THRESHOLD) {
            return "NORMAL";
        }
        
        if (cpuScore == maxScore) {
            return "HIGH_CPU_USAGE";
        } else if (memoryScore == maxScore) {
            return "HIGH_MEMORY_USAGE";
        } else if (responseTimeScore == maxScore) {
            return "PERFORMANCE_DEGRADATION";
        } else if (requestRateScore == maxScore) {
            return "HIGH_REQUEST_RATE";
        } else {
            return "INFRASTRUCTURE_ISSUE";
        }
    }
    
    /**
     * Identify specific anomaly features
     */
    private List<String> identifyAnomalyFeatures(SystemMetrics metrics, double cpuScore, 
                                                double memoryScore, double responseTimeScore, 
                                                double requestRateScore, double customMetricsScore) {
        
        List<String> features = new ArrayList<>();
        
        if (cpuScore > ANOMALY_THRESHOLD) {
            features.add("high_cpu_usage");
        }
        
        if (memoryScore > ANOMALY_THRESHOLD) {
            features.add("high_memory_usage");
        }
        
        if (responseTimeScore > ANOMALY_THRESHOLD) {
            features.add("elevated_response_time");
        }
        
        if (requestRateScore > ANOMALY_THRESHOLD) {
            features.add("high_request_rate");
        }
        
        if (customMetricsScore > ANOMALY_THRESHOLD) {
            Map<String, Object> customMetrics = metrics.getCustomMetrics();
            
            if (customMetrics != null) {
                if (customMetrics.containsKey("disk_usage")) {
                    double diskUsage = ((Number) customMetrics.get("disk_usage")).doubleValue();
                    if (diskUsage > 80.0) {
                        features.add("high_disk_usage");
                    }
                }
                
                if (customMetrics.containsKey("network_latency")) {
                    double networkLatency = ((Number) customMetrics.get("network_latency")).doubleValue();
                    if (networkLatency > 200.0) {
                        features.add("high_network_latency");
                    }
                }
                
                if (customMetrics.containsKey("db_connections")) {
                    double dbConnections = ((Number) customMetrics.get("db_connections")).doubleValue();
                    if (dbConnections > 75.0) {
                        features.add("high_db_connections");
                    }
                }
            }
        }
        
        return features;
    }
    
    /**
     * Build analysis metrics
     */
    private Map<String, Object> buildAnalysisMetrics(double cpuScore, double memoryScore, 
                                                    double responseTimeScore, double requestRateScore, 
                                                    double customMetricsScore, double overallScore) {
        
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("cpu_anomaly_score", Math.round(cpuScore * 100.0) / 100.0);
        metrics.put("memory_anomaly_score", Math.round(memoryScore * 100.0) / 100.0);
        metrics.put("response_time_anomaly_score", Math.round(responseTimeScore * 100.0) / 100.0);
        metrics.put("request_rate_anomaly_score", Math.round(requestRateScore * 100.0) / 100.0);
        metrics.put("custom_metrics_anomaly_score", Math.round(customMetricsScore * 100.0) / 100.0);
        metrics.put("overall_anomaly_score", Math.round(overallScore * 100.0) / 100.0);
        
        // Performance baseline deviation
        metrics.put("baseline_deviation", calculateBaselineDeviation(overallScore));
        
        // Trend analysis
        metrics.put("trend_score", calculateTrendScore(overallScore));
        
        return metrics;
    }
    
    /**
     * Calculate baseline deviation
     */
    private double calculateBaselineDeviation(double currentScore) {
        // Simplified baseline calculation - in production, this would use historical data
        double baseline = 0.2; // Assume 20% is normal baseline
        return Math.abs(currentScore - baseline) / baseline;
    }
    
    /**
     * Calculate trend score
     */
    private double calculateTrendScore(double currentScore) {
        // Simplified trend calculation - in production, this would use time series analysis
        return Math.min(1.0, currentScore * 0.8);
    }
    
    /**
     * Generate unique anomaly ID
     */
    private String generateAnomalyId(LocalDateTime timestamp) {
        return "PERF_" + timestamp.toString().replace(":", "").replace("-", "").replace("T", "_");
    }
}