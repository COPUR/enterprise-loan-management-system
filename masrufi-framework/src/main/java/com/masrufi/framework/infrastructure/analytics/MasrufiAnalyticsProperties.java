package com.masrufi.framework.infrastructure.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * MasruFi Framework Analytics Properties
 * 
 * Configuration properties for Islamic banking analytics:
 * - Analytics data retention policies
 * - Sharia compliance scoring thresholds
 * - Business intelligence refresh intervals
 * - Real-time monitoring configurations
 * - Regulatory reporting schedules
 * - Risk analytics parameters
 * - Customer journey tracking settings
 * - Product performance metrics configurations
 */
@Component
@ConfigurationProperties(prefix = "masrufi.framework.analytics")
public class MasrufiAnalyticsProperties {
    
    /**
     * Whether analytics is enabled
     */
    private boolean enabled = true;
    
    /**
     * Data retention settings
     */
    private DataRetention dataRetention = new DataRetention();
    
    /**
     * Sharia compliance settings
     */
    private ShariaCompliance shariaCompliance = new ShariaCompliance();
    
    /**
     * Business intelligence settings
     */
    private BusinessIntelligence businessIntelligence = new BusinessIntelligence();
    
    /**
     * Real-time monitoring settings
     */
    private RealTimeMonitoring realTimeMonitoring = new RealTimeMonitoring();
    
    /**
     * Regulatory reporting settings
     */
    private RegulatoryReporting regulatoryReporting = new RegulatoryReporting();
    
    /**
     * Risk analytics settings
     */
    private RiskAnalytics riskAnalytics = new RiskAnalytics();
    
    /**
     * Customer journey settings
     */
    private CustomerJourney customerJourney = new CustomerJourney();
    
    /**
     * Product performance settings
     */
    private ProductPerformance productPerformance = new ProductPerformance();
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public DataRetention getDataRetention() { return dataRetention; }
    public void setDataRetention(DataRetention dataRetention) { this.dataRetention = dataRetention; }
    
    public ShariaCompliance getShariaCompliance() { return shariaCompliance; }
    public void setShariaCompliance(ShariaCompliance shariaCompliance) { this.shariaCompliance = shariaCompliance; }
    
    public BusinessIntelligence getBusinessIntelligence() { return businessIntelligence; }
    public void setBusinessIntelligence(BusinessIntelligence businessIntelligence) { this.businessIntelligence = businessIntelligence; }
    
    public RealTimeMonitoring getRealTimeMonitoring() { return realTimeMonitoring; }
    public void setRealTimeMonitoring(RealTimeMonitoring realTimeMonitoring) { this.realTimeMonitoring = realTimeMonitoring; }
    
    public RegulatoryReporting getRegulatoryReporting() { return regulatoryReporting; }
    public void setRegulatoryReporting(RegulatoryReporting regulatoryReporting) { this.regulatoryReporting = regulatoryReporting; }
    
    public RiskAnalytics getRiskAnalytics() { return riskAnalytics; }
    public void setRiskAnalytics(RiskAnalytics riskAnalytics) { this.riskAnalytics = riskAnalytics; }
    
    public CustomerJourney getCustomerJourney() { return customerJourney; }
    public void setCustomerJourney(CustomerJourney customerJourney) { this.customerJourney = customerJourney; }
    
    public ProductPerformance getProductPerformance() { return productPerformance; }
    public void setProductPerformance(ProductPerformance productPerformance) { this.productPerformance = productPerformance; }
    
    // Inner classes for nested properties
    
    public static class DataRetention {
        private Duration transactionAnalytics = Duration.ofDays(365);
        private Duration complianceScores = Duration.ofDays(2555); // 7 years
        private Duration businessMetrics = Duration.ofDays(1095); // 3 years
        private Duration customerJourney = Duration.ofDays(730); // 2 years
        private Duration productPerformance = Duration.ofDays(365);
        private Duration riskAnalytics = Duration.ofDays(1825); // 5 years
        
        // Getters and setters
        public Duration getTransactionAnalytics() { return transactionAnalytics; }
        public void setTransactionAnalytics(Duration transactionAnalytics) { this.transactionAnalytics = transactionAnalytics; }
        
        public Duration getComplianceScores() { return complianceScores; }
        public void setComplianceScores(Duration complianceScores) { this.complianceScores = complianceScores; }
        
        public Duration getBusinessMetrics() { return businessMetrics; }
        public void setBusinessMetrics(Duration businessMetrics) { this.businessMetrics = businessMetrics; }
        
        public Duration getCustomerJourney() { return customerJourney; }
        public void setCustomerJourney(Duration customerJourney) { this.customerJourney = customerJourney; }
        
        public Duration getProductPerformance() { return productPerformance; }
        public void setProductPerformance(Duration productPerformance) { this.productPerformance = productPerformance; }
        
        public Duration getRiskAnalytics() { return riskAnalytics; }
        public void setRiskAnalytics(Duration riskAnalytics) { this.riskAnalytics = riskAnalytics; }
    }
    
    public static class ShariaCompliance {
        private boolean enabled = true;
        private double minimumComplianceScore = 0.90;
        private double ribaThreshold = 0.0;
        private double ghararThreshold = 0.10;
        private double assetBackingThreshold = 0.95;
        private double halalnessThreshold = 0.95;
        private List<String> shariaBoards = List.of("UAE_HIGHER_SHARIA_AUTHORITY", "AAOIFI");
        private Duration refreshInterval = Duration.ofHours(1);
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public double getMinimumComplianceScore() { return minimumComplianceScore; }
        public void setMinimumComplianceScore(double minimumComplianceScore) { this.minimumComplianceScore = minimumComplianceScore; }
        
        public double getRibaThreshold() { return ribaThreshold; }
        public void setRibaThreshold(double ribaThreshold) { this.ribaThreshold = ribaThreshold; }
        
        public double getGhararThreshold() { return ghararThreshold; }
        public void setGhararThreshold(double ghararThreshold) { this.ghararThreshold = ghararThreshold; }
        
        public double getAssetBackingThreshold() { return assetBackingThreshold; }
        public void setAssetBackingThreshold(double assetBackingThreshold) { this.assetBackingThreshold = assetBackingThreshold; }
        
        public double getHalalnessThreshold() { return halalnessThreshold; }
        public void setHalalnessThreshold(double halalnessThreshold) { this.halalnessThreshold = halalnessThreshold; }
        
        public List<String> getShariaBoards() { return shariaBoards; }
        public void setShariaBoards(List<String> shariaBoards) { this.shariaBoards = shariaBoards; }
        
        public Duration getRefreshInterval() { return refreshInterval; }
        public void setRefreshInterval(Duration refreshInterval) { this.refreshInterval = refreshInterval; }
    }
    
    public static class BusinessIntelligence {
        private boolean enabled = true;
        private Duration refreshInterval = Duration.ofMinutes(30);
        private boolean realTimeUpdates = true;
        private List<String> kpiMetrics = List.of(
            "shariaComplianceScore", "islamicProductPortfolio", "halalTransactionVolume",
            "customerSatisfaction", "profitSharingDistribution", "regulatoryCompliance"
        );
        private Map<String, Double> businessTargets = Map.of(
            "complianceScore", 0.95,
            "customerGrowth", 0.15,
            "profitMargin", 0.20
        );
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getRefreshInterval() { return refreshInterval; }
        public void setRefreshInterval(Duration refreshInterval) { this.refreshInterval = refreshInterval; }
        
        public boolean isRealTimeUpdates() { return realTimeUpdates; }
        public void setRealTimeUpdates(boolean realTimeUpdates) { this.realTimeUpdates = realTimeUpdates; }
        
        public List<String> getKpiMetrics() { return kpiMetrics; }
        public void setKpiMetrics(List<String> kpiMetrics) { this.kpiMetrics = kpiMetrics; }
        
        public Map<String, Double> getBusinessTargets() { return businessTargets; }
        public void setBusinessTargets(Map<String, Double> businessTargets) { this.businessTargets = businessTargets; }
    }
    
    public static class RealTimeMonitoring {
        private boolean enabled = true;
        private Duration alertInterval = Duration.ofSeconds(30);
        private List<String> monitoredMetrics = List.of(
            "shariaComplianceViolations", "highValueTransactions", "fraudDetection",
            "systemHealth", "performanceMetrics", "customerEngagement"
        );
        private Map<String, Double> alertThresholds = Map.of(
            "complianceViolation", 0.01,
            "highValueTransaction", 50000.0,
            "fraudScore", 0.80
        );
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getAlertInterval() { return alertInterval; }
        public void setAlertInterval(Duration alertInterval) { this.alertInterval = alertInterval; }
        
        public List<String> getMonitoredMetrics() { return monitoredMetrics; }
        public void setMonitoredMetrics(List<String> monitoredMetrics) { this.monitoredMetrics = monitoredMetrics; }
        
        public Map<String, Double> getAlertThresholds() { return alertThresholds; }
        public void setAlertThresholds(Map<String, Double> alertThresholds) { this.alertThresholds = alertThresholds; }
    }
    
    public static class RegulatoryReporting {
        private boolean enabled = true;
        private Duration reportingInterval = Duration.ofDays(1);
        private List<String> regulatoryBodies = List.of("UAE_CBUAE", "UAE_VARA", "UAE_HSA");
        private List<String> reportTypes = List.of(
            "shariaCompliance", "riskAssessment", "customerDueDiligence",
            "transactionMonitoring", "capitalAdequacy", "liquidityRisk"
        );
        private boolean automaticSubmission = false;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getReportingInterval() { return reportingInterval; }
        public void setReportingInterval(Duration reportingInterval) { this.reportingInterval = reportingInterval; }
        
        public List<String> getRegulatoryBodies() { return regulatoryBodies; }
        public void setRegulatoryBodies(List<String> regulatoryBodies) { this.regulatoryBodies = regulatoryBodies; }
        
        public List<String> getReportTypes() { return reportTypes; }
        public void setReportTypes(List<String> reportTypes) { this.reportTypes = reportTypes; }
        
        public boolean isAutomaticSubmission() { return automaticSubmission; }
        public void setAutomaticSubmission(boolean automaticSubmission) { this.automaticSubmission = automaticSubmission; }
    }
    
    public static class RiskAnalytics {
        private boolean enabled = true;
        private Duration analysisInterval = Duration.ofHours(6);
        private List<String> riskFactors = List.of(
            "creditRisk", "marketRisk", "operationalRisk", "liquidityRisk",
            "shariaComplianceRisk", "reputationalRisk", "concentrationRisk"
        );
        private Map<String, Double> riskThresholds = Map.of(
            "creditRisk", 0.05,
            "marketRisk", 0.08,
            "operationalRisk", 0.03,
            "shariaComplianceRisk", 0.01
        );
        private boolean machineLearningEnabled = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getAnalysisInterval() { return analysisInterval; }
        public void setAnalysisInterval(Duration analysisInterval) { this.analysisInterval = analysisInterval; }
        
        public List<String> getRiskFactors() { return riskFactors; }
        public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }
        
        public Map<String, Double> getRiskThresholds() { return riskThresholds; }
        public void setRiskThresholds(Map<String, Double> riskThresholds) { this.riskThresholds = riskThresholds; }
        
        public boolean isMachineLearningEnabled() { return machineLearningEnabled; }
        public void setMachineLearningEnabled(boolean machineLearningEnabled) { this.machineLearningEnabled = machineLearningEnabled; }
    }
    
    public static class CustomerJourney {
        private boolean enabled = true;
        private Duration trackingInterval = Duration.ofMinutes(5);
        private List<String> journeyStages = List.of(
            "awareness", "consideration", "onboarding", "activation",
            "engagement", "retention", "advocacy"
        );
        private Map<String, Double> conversionTargets = Map.of(
            "onboarding", 0.80,
            "activation", 0.75,
            "retention", 0.85
        );
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getTrackingInterval() { return trackingInterval; }
        public void setTrackingInterval(Duration trackingInterval) { this.trackingInterval = trackingInterval; }
        
        public List<String> getJourneyStages() { return journeyStages; }
        public void setJourneyStages(List<String> journeyStages) { this.journeyStages = journeyStages; }
        
        public Map<String, Double> getConversionTargets() { return conversionTargets; }
        public void setConversionTargets(Map<String, Double> conversionTargets) { this.conversionTargets = conversionTargets; }
    }
    
    public static class ProductPerformance {
        private boolean enabled = true;
        private Duration analysisInterval = Duration.ofHours(2);
        private List<String> islamicProducts = List.of(
            "murabaha", "musharakah", "ijarah", "salam", "istisna", "qardHassan"
        );
        private Map<String, Double> performanceTargets = Map.of(
            "profitMargin", 0.18,
            "customerSatisfaction", 4.5,
            "completionRate", 0.95
        );
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Duration getAnalysisInterval() { return analysisInterval; }
        public void setAnalysisInterval(Duration analysisInterval) { this.analysisInterval = analysisInterval; }
        
        public List<String> getIslamicProducts() { return islamicProducts; }
        public void setIslamicProducts(List<String> islamicProducts) { this.islamicProducts = islamicProducts; }
        
        public Map<String, Double> getPerformanceTargets() { return performanceTargets; }
        public void setPerformanceTargets(Map<String, Double> performanceTargets) { this.performanceTargets = performanceTargets; }
    }
}