package com.bank.infrastructure.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Performance Metrics Collector for Enterprise Banking Platform
 * 
 * Collects and analyzes performance metrics during load testing.
 */
public class PerformanceMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMetricsCollector.class);

    private final Map<String, TestMetrics> testMetrics;
    private final Queue<ResponseTimeRecord> responseTimeHistory;
    private final Queue<ThroughputRecord> throughputHistory;
    private final Instant startTime;

    public PerformanceMetricsCollector() {
        this.testMetrics = new ConcurrentHashMap<>();
        this.responseTimeHistory = new ConcurrentLinkedQueue<>();
        this.throughputHistory = new ConcurrentLinkedQueue<>();
        this.startTime = Instant.now();
    }

    /**
     * Record a response time measurement
     */
    public void recordResponseTime(String testName, long responseTimeMs) {
        TestMetrics metrics = testMetrics.computeIfAbsent(testName, k -> new TestMetrics());
        metrics.addResponseTime(responseTimeMs);
        
        ResponseTimeRecord record = new ResponseTimeRecord(
            Instant.now(), testName, responseTimeMs);
        responseTimeHistory.offer(record);
        
        // Keep only recent history (last 1000 records)
        while (responseTimeHistory.size() > 1000) {
            responseTimeHistory.poll();
        }
    }

    /**
     * Record a request completion
     */
    public void recordRequest(String testName, boolean success) {
        TestMetrics metrics = testMetrics.computeIfAbsent(testName, k -> new TestMetrics());
        
        if (success) {
            metrics.incrementSuccessCount();
        } else {
            metrics.incrementErrorCount();
        }
        
        // Update throughput tracking
        ThroughputRecord record = new ThroughputRecord(Instant.now(), testName);
        throughputHistory.offer(record);
        
        // Keep only recent history (last 10 minutes)
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));
        while (!throughputHistory.isEmpty() && 
               throughputHistory.peek().getTimestamp().isBefore(cutoff)) {
            throughputHistory.poll();
        }
    }

    /**
     * Record error details
     */
    public void recordError(String testName, String errorType, String errorMessage) {
        TestMetrics metrics = testMetrics.computeIfAbsent(testName, k -> new TestMetrics());
        metrics.addError(errorType, errorMessage);
    }

    /**
     * Record resource utilization
     */
    public void recordResourceUtilization(String testName, double cpuUsage, double memoryUsage) {
        TestMetrics metrics = testMetrics.computeIfAbsent(testName, k -> new TestMetrics());
        metrics.addResourceUtilization(cpuUsage, memoryUsage);
    }

    /**
     * Get current throughput (requests per second)
     */
    public double getCurrentThroughput(String testName) {
        Instant oneSecondAgo = Instant.now().minus(Duration.ofSeconds(1));
        
        long requestsInLastSecond = throughputHistory.stream()
            .filter(record -> record.getTestName().equals(testName))
            .filter(record -> record.getTimestamp().isAfter(oneSecondAgo))
            .count();
        
        return requestsInLastSecond;
    }

    /**
     * Get average throughput over a time period
     */
    public double getAverageThroughput(String testName, Duration period) {
        Instant cutoff = Instant.now().minus(period);
        
        long totalRequests = throughputHistory.stream()
            .filter(record -> record.getTestName().equals(testName))
            .filter(record -> record.getTimestamp().isAfter(cutoff))
            .count();
        
        return (double) totalRequests / period.getSeconds();
    }

    /**
     * Calculate percentile response time
     */
    public long getPercentileResponseTime(String testName, double percentile) {
        TestMetrics metrics = testMetrics.get(testName);
        if (metrics == null || metrics.responseTimes.isEmpty()) {
            return 0;
        }
        
        List<Long> sortedTimes = metrics.responseTimes.stream()
            .sorted()
            .collect(Collectors.toList());
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedTimes.size()) - 1;
        index = Math.max(0, Math.min(index, sortedTimes.size() - 1));
        
        return sortedTimes.get(index);
    }

    /**
     * Get error rate for a test
     */
    public double getErrorRate(String testName) {
        TestMetrics metrics = testMetrics.get(testName);
        if (metrics == null) {
            return 0.0;
        }
        
        long totalRequests = metrics.successCount.get() + metrics.errorCount.get();
        if (totalRequests == 0) {
            return 0.0;
        }
        
        return (double) metrics.errorCount.get() / totalRequests;
    }

    /**
     * Get average response time
     */
    public double getAverageResponseTime(String testName) {
        TestMetrics metrics = testMetrics.get(testName);
        if (metrics == null || metrics.responseTimes.isEmpty()) {
            return 0.0;
        }
        
        return metrics.responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0.0);
    }

    /**
     * Get response time trend over time
     */
    public List<ResponseTimeTrend> getResponseTimeTrend(String testName, Duration period) {
        Instant cutoff = Instant.now().minus(period);
        
        Map<Instant, List<Long>> timeGroups = responseTimeHistory.stream()
            .filter(record -> record.getTestName().equals(testName))
            .filter(record -> record.getTimestamp().isAfter(cutoff))
            .collect(Collectors.groupingBy(
                record -> record.getTimestamp().truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
                Collectors.mapping(ResponseTimeRecord::getResponseTime, Collectors.toList())
            ));
        
        return timeGroups.entrySet().stream()
            .map(entry -> {
                double avgResponseTime = entry.getValue().stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
                return new ResponseTimeTrend(entry.getKey(), avgResponseTime);
            })
            .sorted(Comparator.comparing(ResponseTimeTrend::getTimestamp))
            .collect(Collectors.toList());
    }

    /**
     * Get comprehensive test results
     */
    public PerformanceTestResults getResults() {
        PerformanceTestResults results = new PerformanceTestResults();
        results.setStartTime(startTime);
        results.setEndTime(Instant.now());
        
        for (Map.Entry<String, TestMetrics> entry : testMetrics.entrySet()) {
            String testName = entry.getKey();
            TestMetrics metrics = entry.getValue();
            
            TestSummary summary = new TestSummary();
            summary.setTestName(testName);
            summary.setTotalRequests(metrics.successCount.get() + metrics.errorCount.get());
            summary.setSuccessfulRequests(metrics.successCount.get());
            summary.setFailedRequests(metrics.errorCount.get());
            summary.setErrorRate(getErrorRate(testName));
            summary.setAverageResponseTime(getAverageResponseTime(testName));
            summary.setP50ResponseTime(getPercentileResponseTime(testName, 50));
            summary.setP95ResponseTime(getPercentileResponseTime(testName, 95));
            summary.setP99ResponseTime(getPercentileResponseTime(testName, 99));
            summary.setMaxResponseTime(metrics.getMaxResponseTime());
            summary.setMinResponseTime(metrics.getMinResponseTime());
            summary.setAverageThroughput(getAverageThroughput(testName, Duration.ofMinutes(5)));
            summary.setErrorDetails(metrics.getErrorSummary());
            summary.setAverageCpuUsage(metrics.getAverageCpuUsage());
            summary.setAverageMemoryUsage(metrics.getAverageMemoryUsage());
            
            results.addTestSummary(summary);
        }
        
        return results;
    }

    /**
     * Reset all collected metrics
     */
    public void reset() {
        testMetrics.clear();
        responseTimeHistory.clear();
        throughputHistory.clear();
    }

    /**
     * Internal class to track metrics for a specific test
     */
    private static class TestMetrics {
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final Queue<Long> responseTimes = new ConcurrentLinkedQueue<>();
        private final Map<String, AtomicLong> errorTypes = new ConcurrentHashMap<>();
        private final Queue<String> errorMessages = new ConcurrentLinkedQueue<>();
        private final Queue<Double> cpuUsageHistory = new ConcurrentLinkedQueue<>();
        private final Queue<Double> memoryUsageHistory = new ConcurrentLinkedQueue<>();

        void incrementSuccessCount() {
            successCount.incrementAndGet();
        }

        void incrementErrorCount() {
            errorCount.incrementAndGet();
        }

        void addResponseTime(long responseTime) {
            responseTimes.offer(responseTime);
            // Keep only recent response times (last 1000)
            while (responseTimes.size() > 1000) {
                responseTimes.poll();
            }
        }

        void addError(String errorType, String errorMessage) {
            errorTypes.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
            errorMessages.offer(errorMessage);
            // Keep only recent error messages (last 100)
            while (errorMessages.size() > 100) {
                errorMessages.poll();
            }
        }

        void addResourceUtilization(double cpuUsage, double memoryUsage) {
            cpuUsageHistory.offer(cpuUsage);
            memoryUsageHistory.offer(memoryUsage);
            
            // Keep only recent history (last 100 measurements)
            while (cpuUsageHistory.size() > 100) {
                cpuUsageHistory.poll();
            }
            while (memoryUsageHistory.size() > 100) {
                memoryUsageHistory.poll();
            }
        }

        long getMaxResponseTime() {
            return responseTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        }

        long getMinResponseTime() {
            return responseTimes.stream().mapToLong(Long::longValue).min().orElse(0);
        }

        Map<String, Long> getErrorSummary() {
            return errorTypes.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().get()
                ));
        }

        double getAverageCpuUsage() {
            return cpuUsageHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }

        double getAverageMemoryUsage() {
            return memoryUsageHistory.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
    }

    /**
     * Record for tracking response times over time
     */
    private static class ResponseTimeRecord {
        private final Instant timestamp;
        private final String testName;
        private final long responseTime;

        public ResponseTimeRecord(Instant timestamp, String testName, long responseTime) {
            this.timestamp = timestamp;
            this.testName = testName;
            this.responseTime = responseTime;
        }

        public Instant getTimestamp() { return timestamp; }
        public String getTestName() { return testName; }
        public long getResponseTime() { return responseTime; }
    }

    /**
     * Record for tracking throughput over time
     */
    private static class ThroughputRecord {
        private final Instant timestamp;
        private final String testName;

        public ThroughputRecord(Instant timestamp, String testName) {
            this.timestamp = timestamp;
            this.testName = testName;
        }

        public Instant getTimestamp() { return timestamp; }
        public String getTestName() { return testName; }
    }
}