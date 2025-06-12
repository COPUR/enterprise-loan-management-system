package com.bank.loanmanagement.dashboard;

import com.bank.loanmanagement.openai.OpenAiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RiskAnalyticsService {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskAnalyticsService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private OpenAiAssistantService assistantService;
    
    public Map<String, Object> getCurrentRiskMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Overall portfolio risk score calculation
            String portfolioRiskQuery = """
                SELECT 
                    AVG(CASE 
                        WHEN risk_level = 'LOW' THEN 2
                        WHEN risk_level = 'MEDIUM' THEN 5
                        WHEN risk_level = 'HIGH' THEN 8
                        ELSE 5
                    END) as avg_risk_score,
                    COUNT(*) as total_customers,
                    SUM(CASE WHEN account_status = 'ACTIVE' THEN 1 ELSE 0 END) as active_customers
                FROM customers
            """;
            
            Map<String, Object> portfolioData = jdbcTemplate.queryForMap(portfolioRiskQuery);
            
            // Loan performance metrics
            String loanMetricsQuery = """
                SELECT 
                    COUNT(*) as total_loans,
                    SUM(outstanding_amount) as total_outstanding,
                    AVG(days_overdue) as avg_days_overdue,
                    COUNT(CASE WHEN days_overdue > 0 THEN 1 END) as overdue_loans,
                    COUNT(CASE WHEN days_overdue > 30 THEN 1 END) as critical_overdue
                FROM loans 
                WHERE status = 'ACTIVE'
            """;
            
            Map<String, Object> loanData = jdbcTemplate.queryForMap(loanMetricsQuery);
            
            // Calculate key metrics
            BigDecimal avgRiskScore = (BigDecimal) portfolioData.get("avg_risk_score");
            int totalLoans = ((Number) loanData.get("total_loans")).intValue();
            int overdueLoans = ((Number) loanData.get("overdue_loans")).intValue();
            int criticalOverdue = ((Number) loanData.get("critical_overdue")).intValue();
            
            metrics.put("overallRiskScore", avgRiskScore != null ? avgRiskScore.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            metrics.put("totalCustomers", portfolioData.get("total_customers"));
            metrics.put("activeCustomers", portfolioData.get("active_customers"));
            metrics.put("totalLoans", totalLoans);
            metrics.put("totalOutstanding", loanData.get("total_outstanding"));
            metrics.put("overdueLoansCount", overdueLoans);
            metrics.put("criticalOverdueCount", criticalOverdue);
            metrics.put("defaultRate", totalLoans > 0 ? (double) overdueLoans / totalLoans * 100 : 0.0);
            metrics.put("criticalRiskRate", totalLoans > 0 ? (double) criticalOverdue / totalLoans * 100 : 0.0);
            metrics.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return metrics;
            
        } catch (Exception e) {
            logger.error("Error calculating risk metrics", e);
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("error", "Unable to calculate risk metrics");
            fallback.put("timestamp", LocalDateTime.now());
            return fallback;
        }
    }
    
    public Map<String, Object> getPortfolioHealthIndicators() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Credit score distribution
            String creditScoreQuery = """
                SELECT 
                    COUNT(CASE WHEN credit_score >= 800 THEN 1 END) as excellent,
                    COUNT(CASE WHEN credit_score >= 700 AND credit_score < 800 THEN 1 END) as good,
                    COUNT(CASE WHEN credit_score >= 600 AND credit_score < 700 THEN 1 END) as fair,
                    COUNT(CASE WHEN credit_score < 600 THEN 1 END) as poor,
                    AVG(credit_score) as avg_credit_score
                FROM customers
            """;
            
            Map<String, Object> creditData = jdbcTemplate.queryForMap(creditScoreQuery);
            
            // Payment performance
            String paymentQuery = """
                SELECT 
                    AVG(CASE WHEN days_overdue = 0 THEN 1.0 ELSE 0.0 END) * 100 as on_time_rate,
                    AVG(CASE WHEN days_overdue > 30 THEN 1.0 ELSE 0.0 END) * 100 as delinquency_rate,
                    AVG(interest_rate) * 100 as avg_interest_rate
                FROM loans 
                WHERE status = 'ACTIVE'
            """;
            
            Map<String, Object> paymentData = jdbcTemplate.queryForMap(paymentQuery);
            
            // Risk level distribution
            String riskDistributionQuery = """
                SELECT 
                    risk_level,
                    COUNT(*) as count,
                    AVG(credit_limit) as avg_credit_limit
                FROM customers 
                GROUP BY risk_level
            """;
            
            List<Map<String, Object>> riskDistribution = jdbcTemplate.queryForList(riskDistributionQuery);
            
            health.put("creditScoreDistribution", creditData);
            health.put("paymentPerformance", paymentData);
            health.put("riskDistribution", riskDistribution);
            health.put("healthScore", calculateHealthScore(creditData, paymentData));
            health.put("timestamp", LocalDateTime.now());
            
            return health;
            
        } catch (Exception e) {
            logger.error("Error calculating portfolio health", e);
            return Map.of("error", "Unable to calculate portfolio health", "timestamp", LocalDateTime.now());
        }
    }
    
    public Map<String, Object> getCustomerRiskDistribution() {
        try {
            String query = """
                SELECT 
                    c.customer_id,
                    c.first_name || ' ' || c.last_name as full_name,
                    c.credit_score,
                    c.risk_level,
                    c.credit_limit,
                    c.available_credit,
                    COALESCE(l.total_outstanding, 0) as total_outstanding,
                    COALESCE(l.max_days_overdue, 0) as max_days_overdue,
                    CASE 
                        WHEN c.credit_score >= 800 AND COALESCE(l.max_days_overdue, 0) = 0 THEN 'EXCELLENT'
                        WHEN c.credit_score >= 700 AND COALESCE(l.max_days_overdue, 0) <= 5 THEN 'GOOD'
                        WHEN c.credit_score >= 600 AND COALESCE(l.max_days_overdue, 0) <= 15 THEN 'MODERATE'
                        WHEN COALESCE(l.max_days_overdue, 0) > 30 THEN 'CRITICAL'
                        ELSE 'HIGH'
                    END as calculated_risk_category
                FROM customers c
                LEFT JOIN (
                    SELECT 
                        customer_id,
                        SUM(outstanding_amount) as total_outstanding,
                        MAX(days_overdue) as max_days_overdue
                    FROM loans 
                    WHERE status = 'ACTIVE'
                    GROUP BY customer_id
                ) l ON c.customer_id = l.customer_id
                ORDER BY c.credit_score DESC
            """;
            
            List<Map<String, Object>> customers = jdbcTemplate.queryForList(query);
            
            // Group by risk categories for summary
            Map<String, Integer> riskCategoryCounts = new HashMap<>();
            Map<String, BigDecimal> riskCategoryAmounts = new HashMap<>();
            
            for (Map<String, Object> customer : customers) {
                String category = (String) customer.get("calculated_risk_category");
                BigDecimal outstanding = (BigDecimal) customer.get("total_outstanding");
                
                riskCategoryCounts.merge(category, 1, Integer::sum);
                riskCategoryAmounts.merge(category, outstanding != null ? outstanding : BigDecimal.ZERO, BigDecimal::add);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("customerDetails", customers);
            result.put("riskCategorySummary", riskCategoryCounts);
            result.put("riskCategoryAmounts", riskCategoryAmounts);
            result.put("totalCustomers", customers.size());
            result.put("timestamp", LocalDateTime.now());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting customer risk distribution", e);
            return Map.of("error", "Unable to get customer risk distribution", "timestamp", LocalDateTime.now());
        }
    }
    
    public Map<String, Object> getLoanPerformanceTrends() {
        try {
            String trendsQuery = """
                SELECT 
                    l.loan_id,
                    l.customer_id,
                    c.first_name || ' ' || c.last_name as customer_name,
                    l.loan_amount,
                    l.outstanding_amount,
                    l.interest_rate * 100 as interest_rate_percent,
                    l.installment_count,
                    l.days_overdue,
                    l.status,
                    l.created_at,
                    l.due_date,
                    CASE 
                        WHEN l.days_overdue = 0 THEN 'ON_TIME'
                        WHEN l.days_overdue <= 15 THEN 'LATE'
                        WHEN l.days_overdue <= 30 THEN 'DELINQUENT'
                        ELSE 'DEFAULT_RISK'
                    END as performance_status,
                    (l.loan_amount - l.outstanding_amount) as amount_paid,
                    ROUND(((l.loan_amount - l.outstanding_amount) / l.loan_amount * 100), 2) as completion_percentage
                FROM loans l
                JOIN customers c ON l.customer_id = c.customer_id
                WHERE l.status = 'ACTIVE'
                ORDER BY l.days_overdue DESC, l.outstanding_amount DESC
            """;
            
            List<Map<String, Object>> loanTrends = jdbcTemplate.queryForList(trendsQuery);
            
            // Performance metrics aggregation
            Map<String, Integer> performanceStatusCounts = new HashMap<>();
            Map<String, BigDecimal> performanceStatusAmounts = new HashMap<>();
            BigDecimal totalPortfolioValue = BigDecimal.ZERO;
            BigDecimal totalAmountPaid = BigDecimal.ZERO;
            
            for (Map<String, Object> loan : loanTrends) {
                String status = (String) loan.get("performance_status");
                BigDecimal outstanding = (BigDecimal) loan.get("outstanding_amount");
                BigDecimal amountPaid = (BigDecimal) loan.get("amount_paid");
                
                performanceStatusCounts.merge(status, 1, Integer::sum);
                performanceStatusAmounts.merge(status, outstanding, BigDecimal::add);
                totalPortfolioValue = totalPortfolioValue.add(outstanding);
                totalAmountPaid = totalAmountPaid.add(amountPaid != null ? amountPaid : BigDecimal.ZERO);
            }
            
            Map<String, Object> trends = new HashMap<>();
            trends.put("loanDetails", loanTrends);
            trends.put("performanceBreakdown", performanceStatusCounts);
            trends.put("performanceAmounts", performanceStatusAmounts);
            trends.put("totalPortfolioValue", totalPortfolioValue);
            trends.put("totalAmountPaid", totalAmountPaid);
            trends.put("portfolioCompletionRate", 
                totalPortfolioValue.compareTo(BigDecimal.ZERO) > 0 ? 
                totalAmountPaid.divide(totalPortfolioValue.add(totalAmountPaid), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : 
                BigDecimal.ZERO);
            trends.put("timestamp", LocalDateTime.now());
            
            return trends;
            
        } catch (Exception e) {
            logger.error("Error getting loan performance trends", e);
            return Map.of("error", "Unable to get loan performance trends", "timestamp", LocalDateTime.now());
        }
    }
    
    public CompletableFuture<Map<String, Object>> getAiGeneratedInsights() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get current portfolio summary
                String portfolioSummary = getPortfolioSummaryForAI();
                
                // Generate AI insights using OpenAI Assistant
                CompletableFuture<String> aiInsights = assistantService.processBankingQuery(
                    "Based on current portfolio data: " + portfolioSummary + 
                    ". Provide key insights on risk patterns, recommendations for portfolio optimization, and early warning indicators for potential issues.",
                    null
                );
                
                String insights = aiInsights.get();
                
                Map<String, Object> result = new HashMap<>();
                result.put("insights", insights);
                result.put("portfolioSummary", portfolioSummary);
                result.put("generatedAt", LocalDateTime.now());
                result.put("source", "OpenAI GPT-4o Analysis");
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error generating AI insights", e);
                return Map.of(
                    "error", "Unable to generate AI insights: " + e.getMessage(),
                    "timestamp", LocalDateTime.now()
                );
            }
        });
    }
    
    public String getPortfolioSummaryForAI() {
        try {
            Map<String, Object> metrics = getCurrentRiskMetrics();
            Map<String, Object> health = getPortfolioHealthIndicators();
            
            StringBuilder summary = new StringBuilder();
            summary.append("Portfolio Summary: ");
            summary.append(metrics.get("totalCustomers")).append(" customers, ");
            summary.append(metrics.get("totalLoans")).append(" active loans, ");
            summary.append("$").append(metrics.get("totalOutstanding")).append(" outstanding. ");
            summary.append("Default rate: ").append(String.format("%.1f", metrics.get("defaultRate"))).append("%. ");
            summary.append("Risk score: ").append(metrics.get("overallRiskScore")).append("/10. ");
            summary.append("Overdue loans: ").append(metrics.get("overdueLoansCount"));
            
            return summary.toString();
            
        } catch (Exception e) {
            logger.error("Error creating portfolio summary", e);
            return "Portfolio data unavailable for analysis";
        }
    }
    
    public List<Map<String, Object>> getCurrentRiskAlerts() {
        try {
            List<Map<String, Object>> alerts = new ArrayList<>();
            
            // High-risk customers with overdue payments
            String criticalAlertsQuery = """
                SELECT 
                    c.customer_id,
                    c.first_name || ' ' || c.last_name as customer_name,
                    c.credit_score,
                    l.loan_id,
                    l.outstanding_amount,
                    l.days_overdue,
                    'CRITICAL_OVERDUE' as alert_type,
                    'Customer has loan overdue for ' || l.days_overdue || ' days' as alert_message
                FROM customers c
                JOIN loans l ON c.customer_id = l.customer_id
                WHERE l.days_overdue > 30 AND l.status = 'ACTIVE'
                ORDER BY l.days_overdue DESC
            """;
            
            alerts.addAll(jdbcTemplate.queryForList(criticalAlertsQuery));
            
            // Low credit score alerts
            String lowCreditQuery = """
                SELECT 
                    customer_id,
                    first_name || ' ' || last_name as customer_name,
                    credit_score,
                    'LOW_CREDIT_SCORE' as alert_type,
                    'Customer credit score (' || credit_score || ') below recommended minimum' as alert_message
                FROM customers
                WHERE credit_score < 600 AND account_status = 'ACTIVE'
                ORDER BY credit_score ASC
            """;
            
            alerts.addAll(jdbcTemplate.queryForList(lowCreditQuery));
            
            // High credit utilization alerts
            String highUtilizationQuery = """
                SELECT 
                    customer_id,
                    first_name || ' ' || last_name as customer_name,
                    credit_limit,
                    available_credit,
                    'HIGH_UTILIZATION' as alert_type,
                    'Credit utilization exceeds 80%' as alert_message
                FROM customers
                WHERE (credit_limit - available_credit) / credit_limit > 0.8 
                AND credit_limit > 0
                ORDER BY (credit_limit - available_credit) / credit_limit DESC
            """;
            
            alerts.addAll(jdbcTemplate.queryForList(highUtilizationQuery));
            
            // Add timestamps and severity
            for (Map<String, Object> alert : alerts) {
                alert.put("timestamp", LocalDateTime.now());
                String alertType = (String) alert.get("alert_type");
                alert.put("severity", getSeverityLevel(alertType));
            }
            
            return alerts;
            
        } catch (Exception e) {
            logger.error("Error getting risk alerts", e);
            return List.of(Map.of(
                "alert_type", "SYSTEM_ERROR",
                "alert_message", "Unable to retrieve risk alerts",
                "severity", "HIGH",
                "timestamp", LocalDateTime.now()
            ));
        }
    }
    
    public List<Map<String, Object>> getLatestRiskAlerts() {
        List<Map<String, Object>> alerts = getCurrentRiskAlerts();
        return alerts.subList(0, Math.min(alerts.size(), 10)); // Return latest 10 alerts
    }
    
    public Map<String, Object> generateCustomerRiskHeatmap() {
        try {
            String heatmapQuery = """
                SELECT 
                    c.customer_id,
                    c.first_name || ' ' || c.last_name as customer_name,
                    c.credit_score,
                    c.risk_level,
                    c.credit_limit,
                    c.available_credit,
                    COALESCE(l.outstanding_amount, 0) as outstanding_amount,
                    COALESCE(l.days_overdue, 0) as days_overdue,
                    CASE 
                        WHEN c.credit_score < 580 THEN 9
                        WHEN c.credit_score < 600 THEN 7
                        WHEN c.credit_score < 650 THEN 5
                        WHEN c.credit_score < 700 THEN 3
                        WHEN c.credit_score < 750 THEN 2
                        ELSE 1
                    END +
                    CASE 
                        WHEN COALESCE(l.days_overdue, 0) > 60 THEN 5
                        WHEN COALESCE(l.days_overdue, 0) > 30 THEN 3
                        WHEN COALESCE(l.days_overdue, 0) > 15 THEN 2
                        WHEN COALESCE(l.days_overdue, 0) > 0 THEN 1
                        ELSE 0
                    END as risk_heat_score
                FROM customers c
                LEFT JOIN (
                    SELECT 
                        customer_id,
                        SUM(outstanding_amount) as outstanding_amount,
                        MAX(days_overdue) as days_overdue
                    FROM loans 
                    WHERE status = 'ACTIVE'
                    GROUP BY customer_id
                ) l ON c.customer_id = l.customer_id
                ORDER BY risk_heat_score DESC, c.credit_score ASC
            """;
            
            List<Map<String, Object>> heatmapData = jdbcTemplate.queryForList(heatmapQuery);
            
            // Create risk zones
            List<Map<String, Object>> criticalZone = new ArrayList<>();
            List<Map<String, Object>> highZone = new ArrayList<>();
            List<Map<String, Object>> mediumZone = new ArrayList<>();
            List<Map<String, Object>> lowZone = new ArrayList<>();
            
            for (Map<String, Object> customer : heatmapData) {
                int riskScore = ((Number) customer.get("risk_heat_score")).intValue();
                
                if (riskScore >= 10) {
                    criticalZone.add(customer);
                } else if (riskScore >= 7) {
                    highZone.add(customer);
                } else if (riskScore >= 4) {
                    mediumZone.add(customer);
                } else {
                    lowZone.add(customer);
                }
            }
            
            Map<String, Object> heatmap = new HashMap<>();
            heatmap.put("criticalRisk", criticalZone);
            heatmap.put("highRisk", highZone);
            heatmap.put("mediumRisk", mediumZone);
            heatmap.put("lowRisk", lowZone);
            heatmap.put("totalCustomers", heatmapData.size());
            heatmap.put("riskZoneCounts", Map.of(
                "critical", criticalZone.size(),
                "high", highZone.size(),
                "medium", mediumZone.size(),
                "low", lowZone.size()
            ));
            heatmap.put("generatedAt", LocalDateTime.now());
            
            return heatmap;
            
        } catch (Exception e) {
            logger.error("Error generating customer risk heatmap", e);
            return Map.of("error", "Unable to generate risk heatmap", "timestamp", LocalDateTime.now());
        }
    }
    
    public Map<String, Object> getDetailedPortfolioRiskAnalysis() {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            analysis.put("riskMetrics", getCurrentRiskMetrics());
            analysis.put("portfolioHealth", getPortfolioHealthIndicators());
            analysis.put("customerDistribution", getCustomerRiskDistribution());
            analysis.put("performanceTrends", getLoanPerformanceTrends());
            analysis.put("riskAlerts", getCurrentRiskAlerts());
            analysis.put("riskHeatmap", generateCustomerRiskHeatmap());
            
            // Calculate overall portfolio risk assessment
            Map<String, Object> riskMetrics = getCurrentRiskMetrics();
            double defaultRate = (Double) riskMetrics.get("defaultRate");
            double criticalRiskRate = (Double) riskMetrics.get("criticalRiskRate");
            
            String overallAssessment;
            if (defaultRate > 20 || criticalRiskRate > 10) {
                overallAssessment = "HIGH_RISK";
            } else if (defaultRate > 10 || criticalRiskRate > 5) {
                overallAssessment = "MEDIUM_RISK";
            } else {
                overallAssessment = "LOW_RISK";
            }
            
            analysis.put("overallRiskAssessment", overallAssessment);
            analysis.put("analysisTimestamp", LocalDateTime.now());
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error performing detailed portfolio risk analysis", e);
            return Map.of("error", "Unable to perform detailed risk analysis", "timestamp", LocalDateTime.now());
        }
    }
    
    // Helper methods
    private double calculateHealthScore(Map<String, Object> creditData, Map<String, Object> paymentData) {
        try {
            BigDecimal avgCreditScore = (BigDecimal) creditData.get("avg_credit_score");
            BigDecimal onTimeRate = (BigDecimal) paymentData.get("on_time_rate");
            
            double creditComponent = avgCreditScore != null ? 
                Math.min(avgCreditScore.doubleValue() / 850.0 * 50, 50) : 25;
            double paymentComponent = onTimeRate != null ? 
                onTimeRate.doubleValue() / 100.0 * 50 : 25;
            
            return Math.round((creditComponent + paymentComponent) * 10.0) / 10.0;
            
        } catch (Exception e) {
            logger.warn("Error calculating health score", e);
            return 50.0; // Default middle score
        }
    }
    
    private String getSeverityLevel(String alertType) {
        return switch (alertType) {
            case "CRITICAL_OVERDUE" -> "CRITICAL";
            case "LOW_CREDIT_SCORE" -> "HIGH";
            case "HIGH_UTILIZATION" -> "MEDIUM";
            default -> "LOW";
        };
    }
    
    // Additional utility methods for dashboard controller
    public int getTotalCustomersCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Integer.class);
    }
    
    public int getTotalLoansCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'", Integer.class);
    }
    
    public BigDecimal getTotalPortfolioValue() {
        BigDecimal value = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(outstanding_amount), 0) FROM loans WHERE status = 'ACTIVE'", 
            BigDecimal.class
        );
        return value != null ? value : BigDecimal.ZERO;
    }
    
    public double getOverallRiskScore() {
        Map<String, Object> metrics = getCurrentRiskMetrics();
        BigDecimal riskScore = (BigDecimal) metrics.get("overallRiskScore");
        return riskScore != null ? riskScore.doubleValue() : 5.0;
    }
    
    public Map<String, Integer> getRiskDistributionBreakdown() {
        String query = "SELECT risk_level, COUNT(*) as count FROM customers GROUP BY risk_level";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
        
        Map<String, Integer> distribution = new HashMap<>();
        for (Map<String, Object> result : results) {
            distribution.put((String) result.get("risk_level"), ((Number) result.get("count")).intValue());
        }
        
        return distribution;
    }
    
    public double getCurrentDefaultRate() {
        Map<String, Object> metrics = getCurrentRiskMetrics();
        return (Double) metrics.get("defaultRate");
    }
    
    public double getCollectionEfficiency() {
        try {
            BigDecimal result = jdbcTemplate.queryForObject(
                "SELECT AVG(CASE WHEN days_overdue <= 30 THEN 1.0 ELSE 0.0 END) * 100 FROM loans WHERE status = 'ACTIVE'",
                BigDecimal.class
            );
            return result != null ? result.doubleValue() : 0.0;
        } catch (Exception e) {
            logger.warn("Error calculating collection efficiency", e);
            return 0.0;
        }
    }
    
    public Map<String, Object> getRiskTrends(int days) {
        // For now, return current metrics with trend simulation
        // In a real implementation, this would analyze historical data
        Map<String, Object> trends = new HashMap<>();
        trends.put("currentMetrics", getCurrentRiskMetrics());
        trends.put("period", days + " days");
        trends.put("trendDirection", "stable"); // Would be calculated from historical data
        trends.put("timestamp", LocalDateTime.now());
        
        return trends;
    }
    
    public CompletableFuture<Map<String, Object>> performDetailedCustomerRiskAnalysis(String customerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String customerQuery = """
                    SELECT 
                        c.*,
                        COALESCE(l.total_outstanding, 0) as total_outstanding,
                        COALESCE(l.loan_count, 0) as loan_count,
                        COALESCE(l.max_days_overdue, 0) as max_days_overdue
                    FROM customers c
                    LEFT JOIN (
                        SELECT 
                            customer_id,
                            SUM(outstanding_amount) as total_outstanding,
                            COUNT(*) as loan_count,
                            MAX(days_overdue) as max_days_overdue
                        FROM loans 
                        WHERE status = 'ACTIVE'
                        GROUP BY customer_id
                    ) l ON c.customer_id = l.customer_id
                    WHERE c.customer_id = ?
                """;
                
                Map<String, Object> customerData = jdbcTemplate.queryForMap(customerQuery, customerId);
                
                // Use AI to analyze customer risk
                String analysisPrompt = String.format(
                    "Analyze customer risk profile: Credit Score: %s, Risk Level: %s, Outstanding: $%s, Days Overdue: %s",
                    customerData.get("credit_score"),
                    customerData.get("risk_level"),
                    customerData.get("total_outstanding"),
                    customerData.get("max_days_overdue")
                );
                
                CompletableFuture<String> aiAnalysis = assistantService.processBankingQuery(analysisPrompt, customerId);
                
                Map<String, Object> result = new HashMap<>();
                result.put("customerData", customerData);
                result.put("aiAnalysis", aiAnalysis.get());
                result.put("analysisTimestamp", LocalDateTime.now());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Error performing customer risk analysis", e);
                return Map.of("error", "Unable to analyze customer risk", "customerId", customerId);
            }
        });
    }
}