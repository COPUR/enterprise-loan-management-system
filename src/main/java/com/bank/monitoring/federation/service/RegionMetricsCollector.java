package com.bank.monitoring.federation.service;

import com.bank.monitoring.federation.model.RegionMetrics;
import com.bank.monitoring.federation.model.PerformanceAnalytics;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Region Metrics Collector Service
 * Collects and aggregates metrics from individual regions
 */
@Service
public class RegionMetricsCollector {

    private static final Logger logger = LoggerFactory.getLogger(RegionMetricsCollector.class);
    
    // Region endpoints for metrics collection
    private final Map<String, String> regionEndpoints = Map.of(
        "us-east-1", "https://monitoring.us-east-1.bank.internal",
        "eu-west-1", "https://monitoring.eu-west-1.bank.internal",
        "ap-southeast-1", "https://monitoring.ap-southeast-1.bank.internal"
    );
    
    /**
     * Collect metrics from a specific region
     */
    public CompletableFuture<RegionMetrics> collectRegionMetrics(String region) {
        logger.debug("Collecting metrics for region: {}", region);
        
        try {
            return CompletableFuture.supplyAsync(() -> {
                // Simulate metrics collection from region
                Thread.sleep(100 + new Random().nextInt(200)); // Simulate network latency
                
                return createRegionMetrics(region);
            });
            
        } catch (Exception e) {
            logger.error("Error collecting metrics for region {}: {}", region, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Generate performance analytics for multiple regions
     */
    public CompletableFuture<PerformanceAnalytics> generatePerformanceAnalytics(List<String> regions) {
        logger.info("Generating performance analytics for {} regions", regions.size());
        
        try {
            List<CompletableFuture<RegionMetrics>> futures = regions.stream()
                .map(this::collectRegionMetrics)
                .collect(Collectors.toList());
            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<RegionMetrics> allMetrics = futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());
                    
                    return aggregatePerformanceAnalytics(regions, allMetrics);
                });
                
        } catch (Exception e) {
            logger.error("Error generating performance analytics: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Create simulated region metrics
     */
    private RegionMetrics createRegionMetrics(String region) {
        Random random = new Random();
        
        // Generate realistic metrics based on region
        double cpuUsage = 45.0 + random.nextDouble() * 40.0; // 45-85%
        double memoryUsage = 50.0 + random.nextDouble() * 35.0; // 50-85%
        double responseTime = 80.0 + random.nextDouble() * 60.0; // 80-140ms
        int activeConnections = 800 + random.nextInt(1000); // 800-1800
        
        // Banking-specific metrics
        double bankingTransactions = getRegionTransactionVolume(region, random);
        double loanApplications = getRegionLoanApplications(region, random);
        
        Map<String, Double> systemMetrics = Map.of(
            "cpu_usage", cpuUsage,
            "memory_usage", memoryUsage,
            "response_time", responseTime,
            "active_connections", (double) activeConnections
        );
        
        Map<String, Double> bankingMetrics = Map.of(
            "banking_transactions", bankingTransactions,
            "loan_applications", loanApplications
        );
        
        String status = determineRegionStatus(cpuUsage, memoryUsage, responseTime);
        
        return new RegionMetrics(
            region,
            LocalDateTime.now(),
            systemMetrics,
            status,
            bankingMetrics
        );
    }
    
    /**
     * Get region-specific transaction volume
     */
    private double getRegionTransactionVolume(String region, Random random) {
        return switch (region) {
            case "us-east-1" -> 20000.0 + random.nextDouble() * 10000.0; // 20k-30k
            case "eu-west-1" -> 15000.0 + random.nextDouble() * 8000.0;  // 15k-23k
            case "ap-southeast-1" -> 10000.0 + random.nextDouble() * 5000.0; // 10k-15k
            default -> 5000.0 + random.nextDouble() * 3000.0; // 5k-8k
        };
    }
    
    /**
     * Get region-specific loan applications
     */
    private double getRegionLoanApplications(String region, Random random) {
        return switch (region) {
            case "us-east-1" -> 1000.0 + random.nextDouble() * 500.0; // 1k-1.5k
            case "eu-west-1" -> 700.0 + random.nextDouble() * 400.0;  // 700-1.1k
            case "ap-southeast-1" -> 500.0 + random.nextDouble() * 300.0; // 500-800
            default -> 200.0 + random.nextDouble() * 200.0; // 200-400
        };
    }
    
    /**
     * Determine region status based on metrics
     */
    private String determineRegionStatus(double cpuUsage, double memoryUsage, double responseTime) {
        if (cpuUsage > 85.0 || memoryUsage > 85.0 || responseTime > 200.0) {
            return "DEGRADED";
        } else if (cpuUsage > 75.0 || memoryUsage > 75.0 || responseTime > 150.0) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
    
    /**
     * Aggregate performance analytics from multiple regions
     */
    private PerformanceAnalytics aggregatePerformanceAnalytics(List<String> regions, List<RegionMetrics> metrics) {
        double totalThroughput = metrics.stream()
            .mapToDouble(m -> m.getBankingMetrics().get("banking_transactions"))
            .sum();
        
        double avgLatency = metrics.stream()
            .mapToDouble(m -> m.getSystemMetrics().get("response_time"))
            .average()
            .orElse(0.0);
        
        // Calculate P95 latency (simulated)
        double p95Latency = avgLatency * 1.2;
        
        double avgErrorRate = 0.01 + new Random().nextDouble() * 0.02; // 1-3%
        double availability = 99.9 + new Random().nextDouble() * 0.09; // 99.9-99.99%
        
        Map<String, Double> globalMetrics = Map.of(
            "global_throughput", totalThroughput,
            "global_latency_p95", p95Latency,
            "global_error_rate", avgErrorRate,
            "global_availability", availability
        );
        
        Map<String, Map<String, Double>> regionPerformance = new HashMap<>();
        for (RegionMetrics metric : metrics) {
            String region = metric.getRegion();
            double regionThroughput = metric.getBankingMetrics().get("banking_transactions");
            double regionLatency = metric.getSystemMetrics().get("response_time");
            double regionErrorRate = 0.005 + new Random().nextDouble() * 0.02; // 0.5-2.5%
            
            regionPerformance.put(region, Map.of(
                "throughput", regionThroughput,
                "latency_p95", regionLatency * 1.2,
                "error_rate", regionErrorRate
            ));
        }
        
        List<String> insights = generatePerformanceInsights(regions, metrics, regionPerformance);
        
        return new PerformanceAnalytics(
            generateAnalyticsId(),
            LocalDateTime.now(),
            regions,
            globalMetrics,
            regionPerformance,
            insights
        );
    }
    
    /**
     * Generate performance insights
     */
    private List<String> generatePerformanceInsights(List<String> regions, List<RegionMetrics> metrics,
                                                    Map<String, Map<String, Double>> regionPerformance) {
        List<String> insights = new ArrayList<>();
        
        // Find best performing region
        String bestRegion = regionPerformance.entrySet().stream()
            .min(Comparator.comparing(e -> e.getValue().get("latency_p95")))
            .map(Map.Entry::getKey)
            .orElse("unknown");
        
        if (!bestRegion.equals("unknown")) {
            insights.add(bestRegion + " showing best performance");
        }
        
        // Check for performance issues
        for (RegionMetrics metric : metrics) {
            if ("DEGRADED".equals(metric.getRegionStatus())) {
                insights.add(metric.getRegion() + " may need capacity scaling");
            }
        }
        
        // Check traffic distribution
        double totalThroughput = regionPerformance.values().stream()
            .mapToDouble(m -> m.get("throughput"))
            .sum();
        
        boolean balanced = regionPerformance.values().stream()
            .allMatch(m -> Math.abs(m.get("throughput") / totalThroughput - 1.0/regions.size()) < 0.2);
        
        if (balanced) {
            insights.add("Traffic distribution is optimal");
        } else {
            insights.add("Traffic distribution may need rebalancing");
        }
        
        return insights;
    }
    
    /**
     * Generate unique analytics ID
     */
    private String generateAnalyticsId() {
        return "ANALYTICS_" + System.currentTimeMillis();
    }
}