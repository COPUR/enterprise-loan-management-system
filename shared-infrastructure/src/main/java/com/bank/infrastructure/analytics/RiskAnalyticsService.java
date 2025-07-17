package com.bank.infrastructure.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Risk Analytics Service for Enterprise Loan Management System
 * 
 * Provides comprehensive risk analytics and dashboard metrics including:
 * - RA-001: Dashboard metrics calculation (total customers, active loans, portfolio value)
 * - RA-002: Risk distribution analysis (low, medium, high risk categorization)
 * - RA-003: Default rate calculation and monitoring
 * - RA-004: Collection efficiency measurement
 * - RA-005: Real-time risk alerts generation
 * - RA-006: Portfolio performance analysis
 * - RA-007: Credit score-based risk scoring
 * 
 * Risk Categories:
 * - Low Risk: Credit score >= 700
 * - Medium Risk: Credit score 600-699
 * - High Risk: Credit score < 600
 * 
 * Key Metrics:
 * - Risk Score: 10 - ((creditScore - 300) / 55.0)
 * - Default Rate: (Failed Payments / Total Payments) * 100
 * - Collection Efficiency: (Completed Payments / Total Payments) * 100
 */
@Service
public class RiskAnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskAnalyticsService.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    // Risk scoring constants
    private static final int MIN_CREDIT_SCORE = 300;
    private static final int MAX_CREDIT_SCORE = 850;
    private static final double RISK_SCORE_DIVISOR = 55.0;
    private static final double DEFAULT_RISK_SCORE = 7.5;
    
    // Risk thresholds
    private static final int LOW_RISK_THRESHOLD = 700;
    private static final int MEDIUM_RISK_THRESHOLD = 600;
    
    // Alert thresholds
    private static final int HIGH_RISK_LOAN_THRESHOLD = 20;
    private static final int OVERDUE_PAYMENT_THRESHOLD = 10;
    private static final int RECENT_LOAN_THRESHOLD = 30;
    
    public RiskAnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Get comprehensive dashboard overview with risk metrics
     * 
     * @return dashboard overview with KPIs and risk metrics
     * @throws RiskAnalyticsException if calculation fails
     */
    public ObjectNode getDashboardOverview() {
        try {
            logger.debug("Calculating dashboard overview metrics");
            
            ObjectNode dashboard = objectMapper.createObjectNode();
            
            // Basic metrics
            Integer totalCustomers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
            Integer activeLoans = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'", Integer.class);
            BigDecimal portfolioValue = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(amount), 0) FROM loans WHERE status = 'ACTIVE'", BigDecimal.class);
            
            dashboard.put("totalCustomers", totalCustomers != null ? totalCustomers : 0);
            dashboard.put("activeLoans", activeLoans != null ? activeLoans : 0);
            dashboard.put("portfolioValue", portfolioValue != null ? portfolioValue.doubleValue() : 0.0);
            
            // Risk metrics
            ObjectNode riskMetrics = calculateRiskMetrics();
            dashboard.set("riskMetrics", riskMetrics);
            
            // Timestamp
            dashboard.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.debug("Dashboard overview calculated successfully");
            return dashboard;
            
        } catch (Exception e) {
            logger.error("Failed to calculate dashboard overview", e);
            throw new RiskAnalyticsException("Failed to calculate dashboard overview", "DASHBOARD_ERROR", e);
        }
    }
    
    /**
     * Calculate risk distribution across customer base
     * 
     * @return risk distribution with counts and percentages
     */
    public ObjectNode calculateRiskDistribution() {
        try {
            logger.debug("Calculating risk distribution");
            
            ObjectNode riskDistribution = objectMapper.createObjectNode();
            
            Integer lowRisk = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers WHERE credit_score >= " + LOW_RISK_THRESHOLD, Integer.class);
            Integer mediumRisk = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers WHERE credit_score >= " + MEDIUM_RISK_THRESHOLD + " AND credit_score < " + LOW_RISK_THRESHOLD, Integer.class);
            Integer highRisk = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers WHERE credit_score < " + MEDIUM_RISK_THRESHOLD, Integer.class);
            
            int lowRiskCount = lowRisk != null ? lowRisk : 0;
            int mediumRiskCount = mediumRisk != null ? mediumRisk : 0;
            int highRiskCount = highRisk != null ? highRisk : 0;
            int totalCustomers = lowRiskCount + mediumRiskCount + highRiskCount;
            
            riskDistribution.put("lowRisk", lowRiskCount);
            riskDistribution.put("mediumRisk", mediumRiskCount);
            riskDistribution.put("highRisk", highRiskCount);
            riskDistribution.put("totalCustomers", totalCustomers);
            
            // Calculate percentages
            if (totalCustomers > 0) {
                riskDistribution.put("lowRiskPercentage", (lowRiskCount * 100.0) / totalCustomers);
                riskDistribution.put("mediumRiskPercentage", (mediumRiskCount * 100.0) / totalCustomers);
                riskDistribution.put("highRiskPercentage", (highRiskCount * 100.0) / totalCustomers);
            } else {
                riskDistribution.put("lowRiskPercentage", 0.0);
                riskDistribution.put("mediumRiskPercentage", 0.0);
                riskDistribution.put("highRiskPercentage", 0.0);
            }
            
            logger.debug("Risk distribution calculated successfully");
            return riskDistribution;
            
        } catch (Exception e) {
            logger.error("Failed to calculate risk distribution", e);
            throw new RiskAnalyticsException("Failed to calculate risk distribution", "RISK_DISTRIBUTION_ERROR", e);
        }
    }
    
    /**
     * Calculate default rate based on payment failures
     * 
     * @return default rate as percentage
     */
    public double calculateDefaultRate() {
        try {
            logger.debug("Calculating default rate");
            
            Integer totalPayments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments", Integer.class);
            Integer failedPayments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments WHERE status = 'FAILED'", Integer.class);
            
            if (totalPayments == null || totalPayments == 0) {
                return 0.0;
            }
            
            int failed = failedPayments != null ? failedPayments : 0;
            double defaultRate = (failed * 100.0) / totalPayments;
            
            logger.debug("Default rate calculated: {}%", defaultRate);
            return defaultRate;
            
        } catch (Exception e) {
            logger.error("Failed to calculate default rate", e);
            throw new RiskAnalyticsException("Failed to calculate default rate", "DEFAULT_RATE_ERROR", e);
        }
    }
    
    /**
     * Calculate collection efficiency based on completed payments
     * 
     * @return collection efficiency as percentage
     */
    public double calculateCollectionEfficiency() {
        try {
            logger.debug("Calculating collection efficiency");
            
            Integer totalPayments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments", Integer.class);
            Integer completedPayments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments WHERE status = 'COMPLETED'", Integer.class);
            
            if (totalPayments == null || totalPayments == 0) {
                return 0.0;
            }
            
            int completed = completedPayments != null ? completedPayments : 0;
            double collectionEfficiency = (completed * 100.0) / totalPayments;
            
            logger.debug("Collection efficiency calculated: {}%", collectionEfficiency);
            return collectionEfficiency;
            
        } catch (Exception e) {
            logger.error("Failed to calculate collection efficiency", e);
            throw new RiskAnalyticsException("Failed to calculate collection efficiency", "COLLECTION_EFFICIENCY_ERROR", e);
        }
    }
    
    /**
     * Generate real-time risk alerts
     * 
     * @return real-time alerts with severity levels
     */
    public ObjectNode getRealTimeAlerts() {
        try {
            logger.debug("Generating real-time risk alerts");
            
            ObjectNode alerts = objectMapper.createObjectNode();
            
            // High-risk loans
            Integer highRiskLoans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loans l JOIN customers c ON l.customer_id = c.id WHERE c.credit_score < " + MEDIUM_RISK_THRESHOLD,
                Integer.class);
            
            // Overdue payments
            Integer overduePayments = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE status = 'PENDING' AND payment_date < CURRENT_DATE",
                Integer.class);
            
            // Recent loans (last 7 days)
            Integer recentLoans = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE' AND created_at >= CURRENT_DATE - INTERVAL '7 days'",
                Integer.class);
            
            int highRiskCount = highRiskLoans != null ? highRiskLoans : 0;
            int overdueCount = overduePayments != null ? overduePayments : 0;
            int recentCount = recentLoans != null ? recentLoans : 0;
            
            alerts.put("highRiskLoans", highRiskCount);
            alerts.put("overduePayments", overdueCount);
            alerts.put("recentLoans", recentCount);
            
            // Calculate alert severity
            ObjectNode alertSeverity = calculateAlertSeverity(highRiskCount, overdueCount, recentCount);
            alerts.set("alertSeverity", alertSeverity);
            
            alerts.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            logger.debug("Real-time alerts generated successfully");
            return alerts;
            
        } catch (Exception e) {
            logger.error("Failed to generate real-time alerts", e);
            throw new RiskAnalyticsException("Failed to generate real-time alerts", "ALERTS_ERROR", e);
        }
    }
    
    /**
     * Calculate alert severity levels
     * 
     * @param highRiskLoans number of high-risk loans
     * @param overduePayments number of overdue payments
     * @param recentLoans number of recent loans
     * @return alert severity breakdown
     */
    public ObjectNode calculateAlertSeverity(int highRiskLoans, int overduePayments, int recentLoans) {
        ObjectNode severity = objectMapper.createObjectNode();
        
        int highSeverity = 0;
        int mediumSeverity = 0;
        int lowSeverity = 0;
        
        // High-risk loans severity
        if (highRiskLoans > HIGH_RISK_LOAN_THRESHOLD) {
            highSeverity++;
        } else if (highRiskLoans > HIGH_RISK_LOAN_THRESHOLD / 2) {
            mediumSeverity++;
        } else if (highRiskLoans > 0) {
            lowSeverity++;
        }
        
        // Overdue payments severity
        if (overduePayments > OVERDUE_PAYMENT_THRESHOLD) {
            mediumSeverity++;
        } else if (overduePayments > 0) {
            lowSeverity++;
        }
        
        // Recent loans severity
        if (recentLoans > RECENT_LOAN_THRESHOLD) {
            lowSeverity++;
        }
        
        severity.put("high", highSeverity);
        severity.put("medium", mediumSeverity);
        severity.put("low", lowSeverity);
        
        return severity;
    }
    
    /**
     * Get portfolio performance over time
     * 
     * @return portfolio performance with monthly breakdown
     */
    public ObjectNode getPortfolioPerformance() {
        try {
            logger.debug("Calculating portfolio performance");
            
            ObjectNode portfolioPerformance = objectMapper.createObjectNode();
            
            String monthlyQuery = """
                SELECT 
                    TO_CHAR(created_at, 'Month') as month,
                    SUM(amount) as total_amount,
                    COUNT(*) as loan_count
                FROM loans 
                WHERE created_at >= CURRENT_DATE - INTERVAL '6 months'
                GROUP BY TO_CHAR(created_at, 'Month'), DATE_TRUNC('month', created_at)
                ORDER BY DATE_TRUNC('month', created_at)
                """;
            
            List<Map<String, Object>> monthlyData = jdbcTemplate.queryForList(monthlyQuery);
            
            ArrayNode monthlyPerformance = objectMapper.createArrayNode();
            double totalPortfolioValue = 0.0;
            int totalLoans = 0;
            
            for (Map<String, Object> monthData : monthlyData) {
                ObjectNode monthNode = objectMapper.createObjectNode();
                
                String month = (String) monthData.get("month");
                BigDecimal totalAmount = (BigDecimal) monthData.get("total_amount");
                Long loanCount = (Long) monthData.get("loan_count");
                
                monthNode.put("month", month != null ? month.trim() : "");
                monthNode.put("totalAmount", totalAmount != null ? totalAmount.doubleValue() : 0.0);
                monthNode.put("loanCount", loanCount != null ? loanCount.intValue() : 0);
                
                monthlyPerformance.add(monthNode);
                
                totalPortfolioValue += totalAmount != null ? totalAmount.doubleValue() : 0.0;
                totalLoans += loanCount != null ? loanCount.intValue() : 0;
            }
            
            portfolioPerformance.set("monthlyPerformance", monthlyPerformance);
            
            // Calculate summary
            ObjectNode summary = objectMapper.createObjectNode();
            summary.put("totalPortfolioValue", totalPortfolioValue);
            summary.put("totalLoans", totalLoans);
            
            // Calculate average monthly growth (simplified)
            double averageMonthlyGrowth = monthlyData.size() > 1 ? 5.0 : 0.0; // Placeholder calculation
            summary.put("averageMonthlyGrowth", averageMonthlyGrowth);
            
            portfolioPerformance.set("summary", summary);
            
            logger.debug("Portfolio performance calculated successfully");
            return portfolioPerformance;
            
        } catch (Exception e) {
            logger.error("Failed to calculate portfolio performance", e);
            throw new RiskAnalyticsException("Failed to calculate portfolio performance", "PORTFOLIO_ERROR", e);
        }
    }
    
    /**
     * Calculate overall risk score based on average credit score
     * 
     * @return risk score (0-10, where 0 is lowest risk)
     */
    public double calculateRiskScore() {
        try {
            logger.debug("Calculating risk score");
            
            Double avgCreditScore = jdbcTemplate.queryForObject("SELECT AVG(CAST(credit_score AS DECIMAL)) FROM customers", Double.class);
            
            if (avgCreditScore == null) {
                logger.warn("No credit score data available, using default risk score");
                return DEFAULT_RISK_SCORE;
            }
            
            // Risk score formula: 10 - ((creditScore - 300) / 55.0)
            // This transforms 300-850 credit score range to 10-0 risk score range
            double riskScore = 10.0 - ((avgCreditScore - MIN_CREDIT_SCORE) / RISK_SCORE_DIVISOR);
            
            // Ensure risk score is within bounds
            riskScore = Math.max(0.0, Math.min(10.0, riskScore));
            
            logger.debug("Risk score calculated: {}", riskScore);
            return riskScore;
            
        } catch (Exception e) {
            logger.error("Failed to calculate risk score", e);
            throw new RiskAnalyticsException("Failed to calculate risk score", "RISK_SCORE_ERROR", e);
        }
    }
    
    /**
     * Calculate comprehensive risk metrics
     * 
     * @return risk metrics object
     */
    private ObjectNode calculateRiskMetrics() {
        ObjectNode riskMetrics = objectMapper.createObjectNode();
        
        // Risk distribution
        ObjectNode riskDistribution = calculateRiskDistribution();
        riskMetrics.put("lowRisk", riskDistribution.get("lowRisk").asInt());
        riskMetrics.put("mediumRisk", riskDistribution.get("mediumRisk").asInt());
        riskMetrics.put("highRisk", riskDistribution.get("highRisk").asInt());
        
        // Risk score
        double riskScore = calculateRiskScore();
        riskMetrics.put("riskScore", riskScore);
        
        // Default rate
        double defaultRate = calculateDefaultRate();
        riskMetrics.put("defaultRate", defaultRate);
        
        // Collection efficiency
        double collectionEfficiency = calculateCollectionEfficiency();
        riskMetrics.put("collectionEfficiency", collectionEfficiency);
        
        return riskMetrics;
    }
}