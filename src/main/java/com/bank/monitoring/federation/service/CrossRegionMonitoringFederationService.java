package com.bank.monitoring.federation.service;

import com.bank.monitoring.federation.model.*;
import com.bank.monitoring.federation.repository.CrossRegionDataRepository;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

/**
 * Cross-Region Monitoring Federation Service
 * Main orchestration service for multi-region monitoring federation
 */
@Service
public class CrossRegionMonitoringFederationService {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionMonitoringFederationService.class);
    
    private final RegionMetricsCollector regionMetricsCollector;
    private final AlertCorrelationService alertCorrelationService;
    private final DisasterRecoveryMonitoringService disasterRecoveryMonitoringService;
    private final GlobalDashboardService globalDashboardService;
    private final CrossRegionDataRepository crossRegionDataRepository;
    private final RegionHealthMonitor regionHealthMonitor;
    
    public CrossRegionMonitoringFederationService(
            RegionMetricsCollector regionMetricsCollector,
            AlertCorrelationService alertCorrelationService,
            DisasterRecoveryMonitoringService disasterRecoveryMonitoringService,
            GlobalDashboardService globalDashboardService,
            CrossRegionDataRepository crossRegionDataRepository,
            RegionHealthMonitor regionHealthMonitor) {
        this.regionMetricsCollector = regionMetricsCollector;
        this.alertCorrelationService = alertCorrelationService;
        this.disasterRecoveryMonitoringService = disasterRecoveryMonitoringService;
        this.globalDashboardService = globalDashboardService;
        this.crossRegionDataRepository = crossRegionDataRepository;
        this.regionHealthMonitor = regionHealthMonitor;
    }
    
    /**
     * Collect metrics from all regions concurrently
     */
    public CompletableFuture<Map<String, RegionMetrics>> collectAllRegionMetrics(List<String> regions) {
        logger.info("Collecting metrics from {} regions: {}", regions.size(), regions);
        
        try {
            List<CompletableFuture<RegionMetrics>> futures = regions.stream()
                .map(region -> regionMetricsCollector.collectRegionMetrics(region))
                .collect(Collectors.toList());
            
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    Map<String, RegionMetrics> metricsMap = new HashMap<>();
                    
                    for (int i = 0; i < regions.size(); i++) {
                        String region = regions.get(i);
                        try {
                            RegionMetrics metrics = futures.get(i).join();
                            metricsMap.put(region, metrics);
                            logger.debug("Collected metrics for region {}: status={}", 
                                region, metrics.getRegionStatus());
                        } catch (Exception e) {
                            logger.error("Failed to collect metrics for region {}: {}", region, e.getMessage());
                            // Create fallback metrics
                            metricsMap.put(region, createFallbackMetrics(region));
                        }
                    }
                    
                    logger.info("Successfully collected metrics from {} regions", metricsMap.size());
                    return metricsMap;
                });
                
        } catch (Exception e) {
            logger.error("Error collecting region metrics: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Correlate alerts across regions
     */
    public CompletableFuture<AlertCorrelationResult> correlateRegionAlerts(List<RegionAlert> alerts) {
        logger.info("Correlating {} alerts across regions", alerts.size());
        
        try {
            return alertCorrelationService.correlateAlerts(alerts)
                .thenApply(result -> {
                    logger.info("Alert correlation completed: correlationId={}, score={}, affectedRegions={}", 
                        result.getCorrelationId(), result.getCorrelationScore(), result.getAffectedRegions().size());
                    
                    // Store correlation result
                    crossRegionDataRepository.saveAlertCorrelation(result);
                    
                    return result;
                });
                
        } catch (Exception e) {
            logger.error("Error correlating region alerts: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Check disaster recovery status across regions
     */
    public CompletableFuture<DisasterRecoveryStatus> checkDisasterRecoveryStatus(List<String> regions) {
        logger.info("Checking disaster recovery status for {} regions", regions.size());
        
        try {
            return disasterRecoveryMonitoringService.checkDisasterRecoveryStatus(regions)
                .thenApply(status -> {
                    logger.info("Disaster recovery status check completed: statusId={}, overallStatus={}", 
                        status.getStatusId(), status.getOverallStatus());
                    
                    // Store DR status
                    crossRegionDataRepository.saveDisasterRecoveryStatus(status);
                    
                    return status;
                });
                
        } catch (Exception e) {
            logger.error("Error checking disaster recovery status: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Generate unified global dashboard
     */
    public CompletableFuture<GlobalDashboardData> generateGlobalDashboard(List<String> regions) {
        logger.info("Generating global dashboard for {} regions", regions.size());
        
        try {
            return globalDashboardService.generateGlobalDashboard(regions)
                .thenApply(dashboard -> {
                    logger.info("Global dashboard generated: dashboardId={}, regions={}", 
                        dashboard.getDashboardId(), dashboard.getRegions().size());
                    
                    // Store dashboard data
                    crossRegionDataRepository.saveGlobalDashboard(dashboard);
                    
                    return dashboard;
                });
                
        } catch (Exception e) {
            logger.error("Error generating global dashboard: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Handle region failover
     */
    public CompletableFuture<RegionFailoverResult> handleRegionFailover(String failedRegion, List<String> healthyRegions) {
        logger.warn("Handling region failover: failedRegion={}, healthyRegions={}", failedRegion, healthyRegions);
        
        try {
            return disasterRecoveryMonitoringService.handleRegionFailover(failedRegion, healthyRegions)
                .thenApply(result -> {
                    logger.info("Region failover completed: failoverId={}, status={}, duration={}ms", 
                        result.getFailoverId(), result.getFailoverStatus(), 
                        result.getFailoverMetrics().get("failover_duration"));
                    
                    // Store failover result
                    crossRegionDataRepository.saveFailoverResult(result);
                    
                    return result;
                });
                
        } catch (Exception e) {
            logger.error("Error handling region failover: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Check global compliance status
     */
    public CompletableFuture<ComplianceStatus> checkGlobalComplianceStatus(List<String> regions) {
        logger.info("Checking global compliance status for {} regions", regions.size());
        
        try {
            return regionHealthMonitor.checkComplianceStatus(regions)
                .thenApply(status -> {
                    logger.info("Global compliance check completed: complianceId={}, score={}", 
                        status.getComplianceId(), status.getGlobalMetrics().get("global_compliance_score"));
                    
                    // Store compliance status
                    crossRegionDataRepository.saveComplianceStatus(status);
                    
                    return status;
                });
                
        } catch (Exception e) {
            logger.error("Error checking global compliance status: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Generate performance analytics
     */
    public CompletableFuture<PerformanceAnalytics> generatePerformanceAnalytics(List<String> regions) {
        logger.info("Generating performance analytics for {} regions", regions.size());
        
        try {
            return regionMetricsCollector.generatePerformanceAnalytics(regions)
                .thenApply(analytics -> {
                    logger.info("Performance analytics generated: analyticsId={}, globalThroughput={}", 
                        analytics.getAnalyticsId(), analytics.getGlobalMetrics().get("global_throughput"));
                    
                    // Store analytics
                    crossRegionDataRepository.savePerformanceAnalytics(analytics);
                    
                    return analytics;
                });
                
        } catch (Exception e) {
            logger.error("Error generating performance analytics: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Get comprehensive federation status
     */
    public CompletableFuture<FederationStatus> getFederationStatus(List<String> regions) {
        logger.info("Getting comprehensive federation status for {} regions", regions.size());
        
        try {
            CompletableFuture<Map<String, RegionMetrics>> metricsTask = collectAllRegionMetrics(regions);
            CompletableFuture<DisasterRecoveryStatus> drTask = checkDisasterRecoveryStatus(regions);
            CompletableFuture<ComplianceStatus> complianceTask = checkGlobalComplianceStatus(regions);
            CompletableFuture<PerformanceAnalytics> analyticsTask = generatePerformanceAnalytics(regions);
            
            return CompletableFuture.allOf(metricsTask, drTask, complianceTask, analyticsTask)
                .thenApply(v -> {
                    FederationStatus federationStatus = new FederationStatus(
                        generateFederationId(),
                        LocalDateTime.now(),
                        regions,
                        metricsTask.join(),
                        drTask.join(),
                        complianceTask.join(),
                        analyticsTask.join(),
                        calculateOverallFederationHealth(regions, metricsTask.join(), drTask.join())
                    );
                    
                    logger.info("Federation status compiled: federationId={}, overallHealth={}", 
                        federationStatus.getFederationId(), federationStatus.getOverallHealth());
                    
                    return federationStatus;
                });
                
        } catch (Exception e) {
            logger.error("Error getting federation status: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Create fallback metrics for failed region
     */
    private RegionMetrics createFallbackMetrics(String region) {
        return new RegionMetrics(
            region,
            LocalDateTime.now(),
            Map.of(
                "cpu_usage", 0.0,
                "memory_usage", 0.0,
                "active_connections", 0.0,
                "response_time", 0.0
            ),
            "UNAVAILABLE",
            Map.of()
        );
    }
    
    /**
     * Calculate overall federation health
     */
    private String calculateOverallFederationHealth(List<String> regions, 
                                                   Map<String, RegionMetrics> metrics,
                                                   DisasterRecoveryStatus drStatus) {
        try {
            long healthyRegions = metrics.values().stream()
                .filter(m -> "HEALTHY".equals(m.getRegionStatus()))
                .count();
            
            double healthPercentage = (double) healthyRegions / regions.size() * 100;
            
            if (healthPercentage >= 100.0 && "HEALTHY".equals(drStatus.getOverallStatus())) {
                return "EXCELLENT";
            } else if (healthPercentage >= 80.0) {
                return "GOOD";
            } else if (healthPercentage >= 60.0) {
                return "DEGRADED";
            } else {
                return "CRITICAL";
            }
            
        } catch (Exception e) {
            logger.error("Error calculating federation health: {}", e.getMessage());
            return "UNKNOWN";
        }
    }
    
    /**
     * Generate unique federation ID
     */
    private String generateFederationId() {
        return "FED_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}