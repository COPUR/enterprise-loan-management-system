package com.bank.monitoring.federation.service;

import com.bank.monitoring.federation.model.GlobalDashboardData;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Global Dashboard Service
 * Generates unified dashboard data across all regions
 */
@Service
public class GlobalDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(GlobalDashboardService.class);
    
    /**
     * Generate global dashboard data
     */
    public CompletableFuture<GlobalDashboardData> generateGlobalDashboard(List<String> regions) {
        logger.info("Generating global dashboard for {} regions", regions.size());
        
        try {
            return CompletableFuture.supplyAsync(() -> {
                // Simulate dashboard data generation
                Thread.sleep(100 + new Random().nextInt(50));
                
                // Generate global metrics
                Map<String, Double> globalMetrics = generateGlobalMetrics(regions);
                
                // Generate region summaries
                Map<String, Map<String, Object>> regionSummaries = generateRegionSummaries(regions);
                
                // Generate alerts
                List<String> alerts = generateCurrentAlerts(regions);
                
                return new GlobalDashboardData(
                    generateDashboardId(),
                    LocalDateTime.now(),
                    regions,
                    globalMetrics,
                    regionSummaries,
                    alerts
                );
            });
            
        } catch (Exception e) {
            logger.error("Error generating global dashboard: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Generate global metrics
     */
    private Map<String, Double> generateGlobalMetrics(List<String> regions) {
        Random random = new Random();
        
        // Calculate region-based totals
        double totalTransactions = regions.stream()
            .mapToDouble(region -> getRegionTransactionVolume(region, random))
            .sum();
        
        double totalLoanApplications = regions.stream()
            .mapToDouble(region -> getRegionLoanApplications(region, random))
            .sum();
        
        // Calculate global averages
        double globalAvgResponseTime = regions.stream()
            .mapToDouble(region -> 80.0 + random.nextDouble() * 60.0)
            .average()
            .orElse(100.0);
        
        double globalAvgCpuUsage = regions.stream()
            .mapToDouble(region -> 45.0 + random.nextDouble() * 30.0)
            .average()
            .orElse(60.0);
        
        double globalAvgMemoryUsage = regions.stream()
            .mapToDouble(region -> 50.0 + random.nextDouble() * 25.0)
            .average()
            .orElse(65.0);
        
        return Map.of(
            "total_transactions", totalTransactions,
            "total_loan_applications", totalLoanApplications,
            "global_avg_response_time", globalAvgResponseTime,
            "global_avg_cpu_usage", globalAvgCpuUsage,
            "global_avg_memory_usage", globalAvgMemoryUsage,
            "global_throughput", totalTransactions / 60.0, // per minute
            "global_error_rate", 0.01 + random.nextDouble() * 0.02,
            "global_availability", 99.9 + random.nextDouble() * 0.09
        );
    }
    
    /**
     * Generate region summaries
     */
    private Map<String, Map<String, Object>> generateRegionSummaries(List<String> regions) {
        Map<String, Map<String, Object>> summaries = new HashMap<>();
        Random random = new Random();
        
        for (String region : regions) {
            double weight = calculateRegionWeight(region);
            String status = determineRegionStatus(region, random);
            
            Map<String, Object> summary = Map.of(
                "weight", weight,
                "status", status,
                "cpu_usage", 45.0 + random.nextDouble() * 30.0,
                "memory_usage", 50.0 + random.nextDouble() * 25.0,
                "response_time", 80.0 + random.nextDouble() * 60.0,
                "transactions", getRegionTransactionVolume(region, random),
                "active_connections", 800 + random.nextInt(1000),
                "last_update", LocalDateTime.now().minusSeconds(random.nextInt(60)).toString()
            );
            
            summaries.put(region, summary);
        }
        
        return summaries;
    }
    
    /**
     * Generate current alerts
     */
    private List<String> generateCurrentAlerts(List<String> regions) {
        List<String> alerts = new ArrayList<>();
        Random random = new Random();
        
        // Simulate occasional alerts
        if (random.nextDouble() < 0.3) { // 30% chance of alerts
            for (String region : regions) {
                if (random.nextDouble() < 0.2) { // 20% chance per region
                    alerts.add(generateAlertForRegion(region, random));
                }
            }
        }
        
        return alerts;
    }
    
    /**
     * Calculate region weight based on capacity
     */
    private double calculateRegionWeight(String region) {
        return switch (region) {
            case "us-east-1" -> 0.45;
            case "eu-west-1" -> 0.33;
            case "ap-southeast-1" -> 0.22;
            default -> 1.0 / 3.0; // Equal weight for unknown regions
        };
    }
    
    /**
     * Determine region status
     */
    private String determineRegionStatus(String region, Random random) {
        double healthChance = random.nextDouble();
        
        if (healthChance < 0.85) {
            return "HEALTHY";
        } else if (healthChance < 0.95) {
            return "WARNING";
        } else {
            return "DEGRADED";
        }
    }
    
    /**
     * Get region transaction volume
     */
    private double getRegionTransactionVolume(String region, Random random) {
        return switch (region) {
            case "us-east-1" -> 20000.0 + random.nextDouble() * 10000.0;
            case "eu-west-1" -> 15000.0 + random.nextDouble() * 8000.0;
            case "ap-southeast-1" -> 10000.0 + random.nextDouble() * 5000.0;
            default -> 5000.0 + random.nextDouble() * 3000.0;
        };
    }
    
    /**
     * Get region loan applications
     */
    private double getRegionLoanApplications(String region, Random random) {
        return switch (region) {
            case "us-east-1" -> 1000.0 + random.nextDouble() * 500.0;
            case "eu-west-1" -> 700.0 + random.nextDouble() * 400.0;
            case "ap-southeast-1" -> 500.0 + random.nextDouble() * 300.0;
            default -> 200.0 + random.nextDouble() * 200.0;
        };
    }
    
    /**
     * Generate alert for region
     */
    private String generateAlertForRegion(String region, Random random) {
        String[] alertTypes = {
            "High CPU usage",
            "Memory threshold exceeded",
            "Increased response time",
            "Database connection issues",
            "Network latency spike"
        };
        
        String alertType = alertTypes[random.nextInt(alertTypes.length)];
        return String.format("%s: %s detected in region %s", 
            LocalDateTime.now().toString().substring(11, 19), alertType, region);
    }
    
    /**
     * Generate unique dashboard ID
     */
    private String generateDashboardId() {
        return "DASHBOARD_" + System.currentTimeMillis();
    }
}