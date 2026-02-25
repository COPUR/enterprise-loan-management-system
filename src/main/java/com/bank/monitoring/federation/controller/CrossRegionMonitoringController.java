package com.bank.monitoring.federation.controller;

import com.bank.monitoring.federation.model.*;
import com.bank.monitoring.federation.service.CrossRegionMonitoringFederationService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Cross-Region Monitoring Controller
 * REST API for cross-region monitoring federation operations
 */
@RestController
@RequestMapping("/api/v1/federation")
public class CrossRegionMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionMonitoringController.class);
    
    private final CrossRegionMonitoringFederationService federationService;
    
    public CrossRegionMonitoringController(CrossRegionMonitoringFederationService federationService) {
        this.federationService = federationService;
    }
    
    /**
     * Get federation status for all regions
     */
    @GetMapping("/status")
    public CompletableFuture<ResponseEntity<FederationStatus>> getFederationStatus(
            @RequestParam(defaultValue = "us-east-1,eu-west-1,ap-southeast-1") String regions) {
        
        logger.info("Getting federation status for regions: {}", regions);
        
        List<String> regionList = List.of(regions.split(","));
        
        return federationService.getFederationStatus(regionList)
            .thenApply(status -> ResponseEntity.ok(status))
            .exceptionally(ex -> {
                logger.error("Error getting federation status: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Collect metrics from all regions
     */
    @GetMapping("/metrics")
    public CompletableFuture<ResponseEntity<Map<String, RegionMetrics>>> getRegionMetrics(
            @RequestParam(defaultValue = "us-east-1,eu-west-1,ap-southeast-1") String regions) {
        
        logger.info("Collecting metrics for regions: {}", regions);
        
        List<String> regionList = List.of(regions.split(","));
        
        return federationService.collectAllRegionMetrics(regionList)
            .thenApply(metrics -> ResponseEntity.ok(metrics))
            .exceptionally(ex -> {
                logger.error("Error collecting region metrics: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Correlate alerts across regions
     */
    @PostMapping("/alerts/correlate")
    public CompletableFuture<ResponseEntity<AlertCorrelationResult>> correlateAlerts(
            @RequestBody List<RegionAlert> alerts) {
        
        logger.info("Correlating {} alerts across regions", alerts.size());
        
        return federationService.correlateRegionAlerts(alerts)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(ex -> {
                logger.error("Error correlating alerts: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Check disaster recovery status
     */
    @GetMapping("/disaster-recovery/status")
    public CompletableFuture<ResponseEntity<DisasterRecoveryStatus>> getDisasterRecoveryStatus(
            @RequestParam(defaultValue = "us-east-1,eu-west-1,ap-southeast-1") String regions) {
        
        logger.info("Checking disaster recovery status for regions: {}", regions);
        
        List<String> regionList = List.of(regions.split(","));
        
        return federationService.checkDisasterRecoveryStatus(regionList)
            .thenApply(status -> ResponseEntity.ok(status))
            .exceptionally(ex -> {
                logger.error("Error checking disaster recovery status: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Handle region failover
     */
    @PostMapping("/disaster-recovery/failover")
    public CompletableFuture<ResponseEntity<RegionFailoverResult>> handleRegionFailover(
            @RequestParam String failedRegion,
            @RequestParam String healthyRegions) {
        
        logger.warn("Handling failover for failed region: {}", failedRegion);
        
        List<String> healthyRegionList = List.of(healthyRegions.split(","));
        
        return federationService.handleRegionFailover(failedRegion, healthyRegionList)
            .thenApply(result -> ResponseEntity.ok(result))
            .exceptionally(ex -> {
                logger.error("Error handling region failover: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Generate global dashboard
     */
    @GetMapping("/dashboard")
    public CompletableFuture<ResponseEntity<GlobalDashboardData>> getGlobalDashboard(
            @RequestParam(defaultValue = "us-east-1,eu-west-1,ap-southeast-1") String regions) {
        
        logger.info("Generating global dashboard for regions: {}", regions);
        
        List<String> regionList = List.of(regions.split(","));
        
        return federationService.generateGlobalDashboard(regionList)
            .thenApply(dashboard -> ResponseEntity.ok(dashboard))
            .exceptionally(ex -> {
                logger.error("Error generating global dashboard: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Check global compliance status
     */
    @GetMapping("/compliance/status")
    public CompletableFuture<ResponseEntity<ComplianceStatus>> getComplianceStatus(
            @RequestParam(defaultValue = "us-east-1,eu-west-1,ap-southeast-1") String regions) {
        
        logger.info("Checking compliance status for regions: {}", regions);
        
        List<String> regionList = List.of(regions.split(","));
        
        return federationService.checkGlobalComplianceStatus(regionList)
            .thenApply(status -> ResponseEntity.ok(status))
            .exceptionally(ex -> {
                logger.error("Error checking compliance status: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
    
    /**
     * Generate performance analytics
     */
    @GetMapping("/analytics/performance")
    public CompletableFuture<ResponseEntity<PerformanceAnalytics>> getPerformanceAnalytics(
            @RequestParam(defaultValue = "us-east-1,eu-west-1,ap-southeast-1") String regions) {
        
        logger.info("Generating performance analytics for regions: {}", regions);
        
        List<String> regionList = List.of(regions.split(","));
        
        return federationService.generatePerformanceAnalytics(regionList)
            .thenApply(analytics -> ResponseEntity.ok(analytics))
            .exceptionally(ex -> {
                logger.error("Error generating performance analytics: {}", ex.getMessage());
                return ResponseEntity.internalServerError().build();
            });
    }
}