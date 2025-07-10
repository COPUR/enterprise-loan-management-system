package com.bank.monitoring.federation.repository;

import com.bank.monitoring.federation.model.*;

import org.springframework.stereotype.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Cross-Region Data Repository
 * Manages storage and retrieval of cross-region monitoring data
 */
@Repository
public class CrossRegionDataRepository {

    private static final Logger logger = LoggerFactory.getLogger(CrossRegionDataRepository.class);
    
    // In-memory storage for cross-region data (in production, this would be persistent storage)
    private final Map<String, AlertCorrelationResult> alertCorrelations = new ConcurrentHashMap<>();
    private final Map<String, DisasterRecoveryStatus> disasterRecoveryStatuses = new ConcurrentHashMap<>();
    private final Map<String, GlobalDashboardData> globalDashboards = new ConcurrentHashMap<>();
    private final Map<String, RegionFailoverResult> failoverResults = new ConcurrentHashMap<>();
    private final Map<String, ComplianceStatus> complianceStatuses = new ConcurrentHashMap<>();
    private final Map<String, PerformanceAnalytics> performanceAnalytics = new ConcurrentHashMap<>();
    private final Map<String, FederationStatus> federationStatuses = new ConcurrentHashMap<>();
    
    /**
     * Save alert correlation result
     */
    public void saveAlertCorrelation(AlertCorrelationResult correlation) {
        logger.debug("Saving alert correlation: {}", correlation.getCorrelationId());
        
        try {
            alertCorrelations.put(correlation.getCorrelationId(), correlation);
            logger.info("Alert correlation saved: {}", correlation.getCorrelationId());
        } catch (Exception e) {
            logger.error("Error saving alert correlation {}: {}", correlation.getCorrelationId(), e.getMessage());
        }
    }
    
    /**
     * Save disaster recovery status
     */
    public void saveDisasterRecoveryStatus(DisasterRecoveryStatus status) {
        logger.debug("Saving disaster recovery status: {}", status.getStatusId());
        
        try {
            disasterRecoveryStatuses.put(status.getStatusId(), status);
            logger.info("Disaster recovery status saved: {}", status.getStatusId());
        } catch (Exception e) {
            logger.error("Error saving disaster recovery status {}: {}", status.getStatusId(), e.getMessage());
        }
    }
    
    /**
     * Save global dashboard data
     */
    public void saveGlobalDashboard(GlobalDashboardData dashboard) {
        logger.debug("Saving global dashboard: {}", dashboard.getDashboardId());
        
        try {
            globalDashboards.put(dashboard.getDashboardId(), dashboard);
            logger.info("Global dashboard saved: {}", dashboard.getDashboardId());
        } catch (Exception e) {
            logger.error("Error saving global dashboard {}: {}", dashboard.getDashboardId(), e.getMessage());
        }
    }
    
    /**
     * Save failover result
     */
    public void saveFailoverResult(RegionFailoverResult result) {
        logger.debug("Saving failover result: {}", result.getFailoverId());
        
        try {
            failoverResults.put(result.getFailoverId(), result);
            logger.info("Failover result saved: {}", result.getFailoverId());
        } catch (Exception e) {
            logger.error("Error saving failover result {}: {}", result.getFailoverId(), e.getMessage());
        }
    }
    
    /**
     * Save compliance status
     */
    public void saveComplianceStatus(ComplianceStatus status) {
        logger.debug("Saving compliance status: {}", status.getComplianceId());
        
        try {
            complianceStatuses.put(status.getComplianceId(), status);
            logger.info("Compliance status saved: {}", status.getComplianceId());
        } catch (Exception e) {
            logger.error("Error saving compliance status {}: {}", status.getComplianceId(), e.getMessage());
        }
    }
    
    /**
     * Save performance analytics
     */
    public void savePerformanceAnalytics(PerformanceAnalytics analytics) {
        logger.debug("Saving performance analytics: {}", analytics.getAnalyticsId());
        
        try {
            performanceAnalytics.put(analytics.getAnalyticsId(), analytics);
            logger.info("Performance analytics saved: {}", analytics.getAnalyticsId());
        } catch (Exception e) {
            logger.error("Error saving performance analytics {}: {}", analytics.getAnalyticsId(), e.getMessage());
        }
    }
    
    /**
     * Save federation status
     */
    public void saveFederationStatus(FederationStatus status) {
        logger.debug("Saving federation status: {}", status.getFederationId());
        
        try {
            federationStatuses.put(status.getFederationId(), status);
            logger.info("Federation status saved: {}", status.getFederationId());
        } catch (Exception e) {
            logger.error("Error saving federation status {}: {}", status.getFederationId(), e.getMessage());
        }
    }
    
    /**
     * Get recent alert correlations
     */
    public List<AlertCorrelationResult> getRecentAlertCorrelations(int limit) {
        logger.debug("Getting recent alert correlations (limit: {})", limit);
        
        try {
            return alertCorrelations.values().stream()
                .sorted(Comparator.comparing(AlertCorrelationResult::getAnalysisTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent alert correlations: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recent disaster recovery statuses
     */
    public List<DisasterRecoveryStatus> getRecentDisasterRecoveryStatuses(int limit) {
        logger.debug("Getting recent disaster recovery statuses (limit: {})", limit);
        
        try {
            return disasterRecoveryStatuses.values().stream()
                .sorted(Comparator.comparing(DisasterRecoveryStatus::getCheckTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent disaster recovery statuses: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recent global dashboards
     */
    public List<GlobalDashboardData> getRecentGlobalDashboards(int limit) {
        logger.debug("Getting recent global dashboards (limit: {})", limit);
        
        try {
            return globalDashboards.values().stream()
                .sorted(Comparator.comparing(GlobalDashboardData::getGenerationTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent global dashboards: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recent failover results
     */
    public List<RegionFailoverResult> getRecentFailoverResults(int limit) {
        logger.debug("Getting recent failover results (limit: {})", limit);
        
        try {
            return failoverResults.values().stream()
                .sorted(Comparator.comparing(RegionFailoverResult::getFailoverTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent failover results: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recent compliance statuses
     */
    public List<ComplianceStatus> getRecentComplianceStatuses(int limit) {
        logger.debug("Getting recent compliance statuses (limit: {})", limit);
        
        try {
            return complianceStatuses.values().stream()
                .sorted(Comparator.comparing(ComplianceStatus::getCheckTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent compliance statuses: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recent performance analytics
     */
    public List<PerformanceAnalytics> getRecentPerformanceAnalytics(int limit) {
        logger.debug("Getting recent performance analytics (limit: {})", limit);
        
        try {
            return performanceAnalytics.values().stream()
                .sorted(Comparator.comparing(PerformanceAnalytics::getAnalysisTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent performance analytics: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get recent federation statuses
     */
    public List<FederationStatus> getRecentFederationStatuses(int limit) {
        logger.debug("Getting recent federation statuses (limit: {})", limit);
        
        try {
            return federationStatuses.values().stream()
                .sorted(Comparator.comparing(FederationStatus::getStatusTime).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting recent federation statuses: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get statistics
     */
    public Map<String, Long> getStatistics() {
        logger.debug("Getting repository statistics");
        
        try {
            return Map.of(
                "alert_correlations", (long) alertCorrelations.size(),
                "disaster_recovery_statuses", (long) disasterRecoveryStatuses.size(),
                "global_dashboards", (long) globalDashboards.size(),
                "failover_results", (long) failoverResults.size(),
                "compliance_statuses", (long) complianceStatuses.size(),
                "performance_analytics", (long) performanceAnalytics.size(),
                "federation_statuses", (long) federationStatuses.size()
            );
        } catch (Exception e) {
            logger.error("Error getting statistics: {}", e.getMessage());
            return Map.of();
        }
    }
    
    /**
     * Cleanup old data (retain last 24 hours)
     */
    public void cleanupOldData() {
        logger.info("Cleaning up old cross-region data");
        
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
            
            // Cleanup alert correlations
            alertCorrelations.entrySet().removeIf(entry -> 
                entry.getValue().getAnalysisTime().isBefore(cutoff));
            
            // Cleanup disaster recovery statuses
            disasterRecoveryStatuses.entrySet().removeIf(entry -> 
                entry.getValue().getCheckTime().isBefore(cutoff));
            
            // Cleanup global dashboards
            globalDashboards.entrySet().removeIf(entry -> 
                entry.getValue().getGenerationTime().isBefore(cutoff));
            
            // Cleanup failover results
            failoverResults.entrySet().removeIf(entry -> 
                entry.getValue().getFailoverTime().isBefore(cutoff));
            
            // Cleanup compliance statuses
            complianceStatuses.entrySet().removeIf(entry -> 
                entry.getValue().getCheckTime().isBefore(cutoff));
            
            // Cleanup performance analytics
            performanceAnalytics.entrySet().removeIf(entry -> 
                entry.getValue().getAnalysisTime().isBefore(cutoff));
            
            // Cleanup federation statuses
            federationStatuses.entrySet().removeIf(entry -> 
                entry.getValue().getStatusTime().isBefore(cutoff));
            
            logger.info("Old data cleanup completed");
            
        } catch (Exception e) {
            logger.error("Error during data cleanup: {}", e.getMessage());
        }
    }
}