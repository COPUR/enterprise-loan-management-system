package com.loanmanagement.performance;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Performance Metrics Collector
 * Collects and analyzes performance metrics during load testing
 */
@Slf4j
@Getter
public class PerformanceMetricsCollector {
    
    private final Map<String, Queue<Long>> responseTimesMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCountsMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCountsMap = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> successCountsMap = new ConcurrentHashMap<>();
    
    private LocalDateTime testStartTime;
    private LocalDateTime testEndTime;
    
    /**
     * Start metrics collection
     */
    public void startCollection() {
        testStartTime = LocalDateTime.now();
        clearMetrics();
        log.info("Performance metrics collection started at: {}", testStartTime);
    }
    
    /**
     * Stop metrics collection
     */
    public void stopCollection() {
        testEndTime = LocalDateTime.now();
        log.info("Performance metrics collection stopped at: {}", testEndTime);
    }
    
    /**
     * Record response time for an operation
     */
    public void recordResponseTime(String operation, long responseTimeMs) {
        responseTimesMap.computeIfAbsent(operation, k -> new ConcurrentLinkedQueue<>())
                      .offer(responseTimeMs);
        requestCountsMap.computeIfAbsent(operation, k -> new AtomicLong(0))
                       .incrementAndGet();
    }
    
    /**
     * Record successful operation
     */
    public void recordSuccess(String operation) {
        successCountsMap.computeIfAbsent(operation, k -> new AtomicLong(0))
                       .incrementAndGet();
    }
    
    /**
     * Record failed operation
     */
    public void recordError(String operation) {
        errorCountsMap.computeIfAbsent(operation, k -> new AtomicLong(0))
                     .incrementAndGet();
    }
    
    /**
     * Record operation with timing
     */
    public void recordOperation(String operation, long responseTimeMs, boolean success) {
        recordResponseTime(operation, responseTimeMs);
        if (success) {
            recordSuccess(operation);
        } else {
            recordError(operation);
        }
    }
    
    /**
     * Generate comprehensive performance report
     */
    public PerformanceReport generateReport() {
        Duration testDuration = testStartTime != null && testEndTime != null ? 
                Duration.between(testStartTime, testEndTime) : Duration.ZERO;
        
        Map<String, OperationMetrics> operationMetrics = new HashMap<>();
        
        for (String operation : getAllOperations()) {
            operationMetrics.put(operation, calculateOperationMetrics(operation));
        }
        
        return new PerformanceReport(
                testStartTime,
                testEndTime,
                testDuration,
                operationMetrics,
                calculateOverallMetrics()
        );
    }
    
    /**
     * Clear all collected metrics
     */
    public void clearMetrics() {
        responseTimesMap.clear();
        requestCountsMap.clear();
        errorCountsMap.clear();
        successCountsMap.clear();
    }
    
    /**
     * Get current throughput (requests per second) for an operation
     */
    public double getCurrentThroughput(String operation) {
        if (testStartTime == null) return 0.0;
        
        long totalRequests = requestCountsMap.getOrDefault(operation, new AtomicLong(0)).get();
        Duration elapsed = Duration.between(testStartTime, LocalDateTime.now());
        
        if (elapsed.toSeconds() == 0) return 0.0;
        
        return (double) totalRequests / elapsed.toSeconds();
    }
    
    /**
     * Get current error rate for an operation
     */
    public double getCurrentErrorRate(String operation) {
        long totalRequests = requestCountsMap.getOrDefault(operation, new AtomicLong(0)).get();
        long errors = errorCountsMap.getOrDefault(operation, new AtomicLong(0)).get();
        
        if (totalRequests == 0) return 0.0;
        
        return (double) errors / totalRequests * 100.0;
    }
    
    private Set<String> getAllOperations() {
        Set<String> operations = new HashSet<>();
        operations.addAll(responseTimesMap.keySet());
        operations.addAll(requestCountsMap.keySet());
        operations.addAll(errorCountsMap.keySet());
        operations.addAll(successCountsMap.keySet());
        return operations;
    }
    
    private OperationMetrics calculateOperationMetrics(String operation) {
        Queue<Long> responseTimes = responseTimesMap.getOrDefault(operation, new ConcurrentLinkedQueue<>());
        List<Long> times = new ArrayList<>(responseTimes);
        
        long totalRequests = requestCountsMap.getOrDefault(operation, new AtomicLong(0)).get();
        long successCount = successCountsMap.getOrDefault(operation, new AtomicLong(0)).get();
        long errorCount = errorCountsMap.getOrDefault(operation, new AtomicLong(0)).get();
        
        if (times.isEmpty()) {
            return new OperationMetrics(operation, 0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0);
        }
        
        times.sort(Long::compareTo);
        
        long minResponseTime = times.get(0);
        long maxResponseTime = times.get(times.size() - 1);
        double avgResponseTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p50 = calculatePercentile(times, 50);
        long p95 = calculatePercentile(times, 95);
        long p99 = calculatePercentile(times, 99);
        
        double errorRate = totalRequests > 0 ? (double) errorCount / totalRequests * 100.0 : 0.0;
        double successRate = totalRequests > 0 ? (double) successCount / totalRequests * 100.0 : 0.0;
        
        return new OperationMetrics(
                operation,
                totalRequests,
                successCount,
                errorCount,
                minResponseTime,
                maxResponseTime,
                (long) avgResponseTime,
                p50,
                p95,
                p99,
                errorRate,
                successRate
        );
    }
    
    private OverallMetrics calculateOverallMetrics() {
        long totalRequests = requestCountsMap.values().stream().mapToLong(AtomicLong::get).sum();
        long totalSuccess = successCountsMap.values().stream().mapToLong(AtomicLong::get).sum();
        long totalErrors = errorCountsMap.values().stream().mapToLong(AtomicLong::get).sum();
        
        double overallErrorRate = totalRequests > 0 ? (double) totalErrors / totalRequests * 100.0 : 0.0;
        double overallSuccessRate = totalRequests > 0 ? (double) totalSuccess / totalRequests * 100.0 : 0.0;
        
        Duration testDuration = testStartTime != null && testEndTime != null ? 
                Duration.between(testStartTime, testEndTime) : Duration.ZERO;
        
        double throughput = testDuration.toSeconds() > 0 ? (double) totalRequests / testDuration.toSeconds() : 0.0;
        
        return new OverallMetrics(
                totalRequests,
                totalSuccess,
                totalErrors,
                overallErrorRate,
                overallSuccessRate,
                throughput,
                testDuration.toMillis()
        );
    }
    
    private long calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        
        return sortedValues.get(index);
    }
    
    // Data classes for metrics
    
    public record OperationMetrics(
            String operation,
            long totalRequests,
            long successCount,
            long errorCount,
            long minResponseTime,
            long maxResponseTime,
            long avgResponseTime,
            long p50ResponseTime,
            long p95ResponseTime,
            long p99ResponseTime,
            double errorRate,
            double successRate
    ) {}
    
    public record OverallMetrics(
            long totalRequests,
            long totalSuccess,
            long totalErrors,
            double overallErrorRate,
            double overallSuccessRate,
            double throughput,
            long testDurationMs
    ) {}
    
    public record PerformanceReport(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Duration testDuration,
            Map<String, OperationMetrics> operationMetrics,
            OverallMetrics overallMetrics
    ) {
        public String generateSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Performance Test Report\n");
            summary.append("======================\n");
            summary.append(String.format("Test Duration: %d seconds\n", testDuration.toSeconds()));
            summary.append(String.format("Total Requests: %d\n", overallMetrics.totalRequests()));
            summary.append(String.format("Success Rate: %.2f%%\n", overallMetrics.overallSuccessRate()));
            summary.append(String.format("Error Rate: %.2f%%\n", overallMetrics.overallErrorRate()));
            summary.append(String.format("Overall Throughput: %.2f req/sec\n", overallMetrics.throughput()));
            summary.append("\nOperation Details:\n");
            
            operationMetrics.forEach((operation, metrics) -> {
                summary.append(String.format("\n%s:\n", operation));
                summary.append(String.format("  Requests: %d\n", metrics.totalRequests()));
                summary.append(String.format("  Avg Response Time: %d ms\n", metrics.avgResponseTime()));
                summary.append(String.format("  P95 Response Time: %d ms\n", metrics.p95ResponseTime()));
                summary.append(String.format("  P99 Response Time: %d ms\n", metrics.p99ResponseTime()));
                summary.append(String.format("  Error Rate: %.2f%%\n", metrics.errorRate()));
            });
            
            return summary.toString();
        }
    }
}