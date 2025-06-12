package com.bank.loanmanagement.dashboard;

import com.bank.loanmanagement.openai.OpenAiAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class RiskDashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(RiskDashboardController.class);
    
    @Autowired
    private OpenAiAssistantService assistantService;
    
    @Autowired
    private RiskAnalyticsService riskAnalyticsService;
    
    // Real-time data streaming for dashboard updates
    private final Sinks.Many<Map<String, Object>> dashboardUpdatesSink = 
        Sinks.many().multicast().onBackpressureBuffer();
    
    // GraphQL Queries for Dashboard Data
    @QueryMapping
    public CompletableFuture<Map<String, Object>> riskDashboardData() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> dashboardData = new HashMap<>();
                
                // Real-time risk metrics
                dashboardData.put("riskMetrics", riskAnalyticsService.getCurrentRiskMetrics());
                
                // Portfolio health indicators
                dashboardData.put("portfolioHealth", riskAnalyticsService.getPortfolioHealthIndicators());
                
                // Customer risk distribution
                dashboardData.put("customerRiskDistribution", riskAnalyticsService.getCustomerRiskDistribution());
                
                // Loan performance trends
                dashboardData.put("loanPerformanceTrends", riskAnalyticsService.getLoanPerformanceTrends());
                
                // AI insights summary
                dashboardData.put("aiInsights", riskAnalyticsService.getAiGeneratedInsights());
                
                // Alert notifications
                dashboardData.put("riskAlerts", riskAnalyticsService.getCurrentRiskAlerts());
                
                dashboardData.put("timestamp", LocalDateTime.now());
                dashboardData.put("status", "active");
                
                return dashboardData;
                
            } catch (Exception e) {
                logger.error("Error fetching dashboard data", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to fetch dashboard data: " + e.getMessage());
                errorResponse.put("timestamp", LocalDateTime.now());
                return errorResponse;
            }
        });
    }
    
    @QueryMapping
    public CompletableFuture<Map<String, Object>> customerRiskHeatmap() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return riskAnalyticsService.generateCustomerRiskHeatmap();
            } catch (Exception e) {
                logger.error("Error generating risk heatmap", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to generate heatmap: " + e.getMessage());
                return errorResponse;
            }
        });
    }
    
    @QueryMapping
    public CompletableFuture<Map<String, Object>> portfolioRiskAnalysis() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return riskAnalyticsService.getDetailedPortfolioRiskAnalysis();
            } catch (Exception e) {
                logger.error("Error analyzing portfolio risk", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to analyze portfolio risk: " + e.getMessage());
                return errorResponse;
            }
        });
    }
    
    @QueryMapping
    public CompletableFuture<Map<String, Object>> aiRiskRecommendations() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get AI-powered risk recommendations using OpenAI Assistant
                String portfolioSummary = riskAnalyticsService.getPortfolioSummaryForAI();
                
                CompletableFuture<String> aiAnalysis = assistantService.processBankingQuery(
                    "Analyze current portfolio risk and provide actionable recommendations for risk mitigation", 
                    null
                );
                
                String recommendations = aiAnalysis.get();
                
                Map<String, Object> response = new HashMap<>();
                response.put("recommendations", recommendations);
                response.put("portfolioSummary", portfolioSummary);
                response.put("generatedAt", LocalDateTime.now());
                response.put("confidence", "high");
                
                return response;
                
            } catch (Exception e) {
                logger.error("Error generating AI recommendations", e);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to generate AI recommendations: " + e.getMessage());
                return errorResponse;
            }
        });
    }
    
    // GraphQL Subscriptions for Real-Time Updates
    @SubscriptionMapping
    public Flux<Map<String, Object>> riskDashboardUpdates() {
        // Emit real-time updates every 30 seconds
        return Flux.interval(Duration.ofSeconds(30))
            .map(tick -> {
                Map<String, Object> update = new HashMap<>();
                update.put("timestamp", LocalDateTime.now());
                update.put("updateType", "risk_metrics");
                update.put("data", riskAnalyticsService.getCurrentRiskMetrics());
                return update;
            })
            .mergeWith(dashboardUpdatesSink.asFlux());
    }
    
    @SubscriptionMapping
    public Flux<Map<String, Object>> riskAlerts() {
        // Real-time risk alert notifications
        return Flux.interval(Duration.ofMinutes(1))
            .map(tick -> {
                Map<String, Object> alerts = new HashMap<>();
                alerts.put("timestamp", LocalDateTime.now());
                alerts.put("alerts", riskAnalyticsService.getLatestRiskAlerts());
                return alerts;
            });
    }
    
    // REST API Endpoints for Dashboard Integration
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();
            
            // Key performance indicators
            overview.put("totalCustomers", riskAnalyticsService.getTotalCustomersCount());
            overview.put("totalLoans", riskAnalyticsService.getTotalLoansCount());
            overview.put("portfolioValue", riskAnalyticsService.getTotalPortfolioValue());
            overview.put("riskScore", riskAnalyticsService.getOverallRiskScore());
            
            // Risk distribution
            overview.put("riskDistribution", riskAnalyticsService.getRiskDistributionBreakdown());
            
            // Performance metrics
            overview.put("defaultRate", riskAnalyticsService.getCurrentDefaultRate());
            overview.put("collectionEfficiency", riskAnalyticsService.getCollectionEfficiency());
            
            overview.put("lastUpdated", LocalDateTime.now());
            
            return ResponseEntity.ok(overview);
            
        } catch (Exception e) {
            logger.error("Error fetching dashboard overview", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch overview: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/risk-trends")
    public ResponseEntity<Map<String, Object>> getRiskTrends(
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> trends = riskAnalyticsService.getRiskTrends(days);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            logger.error("Error fetching risk trends", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch risk trends: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/analyze-customer")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> analyzeCustomerRisk(
            @RequestBody Map<String, String> request) {
        try {
            String customerId = request.get("customerId");
            
            if (customerId == null || customerId.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Customer ID is required");
                return ResponseEntity.badRequest().body(CompletableFuture.completedFuture(errorResponse));
            }
            
            CompletableFuture<Map<String, Object>> analysis = riskAnalyticsService
                .performDetailedCustomerRiskAnalysis(customerId);
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            logger.error("Error analyzing customer risk", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to analyze customer risk: " + e.getMessage());
            return ResponseEntity.internalServerError().body(CompletableFuture.completedFuture(errorResponse));
        }
    }
    
    @PostMapping("/trigger-ai-analysis")
    public ResponseEntity<CompletableFuture<Map<String, Object>>> triggerAiAnalysis(
            @RequestBody Map<String, Object> request) {
        try {
            String analysisType = (String) request.get("analysisType");
            String scope = (String) request.getOrDefault("scope", "portfolio");
            
            CompletableFuture<Map<String, Object>> aiAnalysis = assistantService
                .getBankingInsights("LAST_30_DAYS")
                .thenCompose(insights -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("analysisType", analysisType);
                    result.put("scope", scope);
                    result.put("insights", insights);
                    result.put("timestamp", LocalDateTime.now());
                    return CompletableFuture.completedFuture(result);
                });
            
            return ResponseEntity.ok(aiAnalysis);
            
        } catch (Exception e) {
            logger.error("Error triggering AI analysis", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to trigger AI analysis: " + e.getMessage());
            return ResponseEntity.internalServerError().body(CompletableFuture.completedFuture(errorResponse));
        }
    }
    
    @GetMapping("/export-data")
    public ResponseEntity<Map<String, Object>> exportDashboardData(
            @RequestParam(defaultValue = "json") String format) {
        try {
            Map<String, Object> exportData = new HashMap<>();
            
            // Compile all dashboard data for export
            exportData.put("riskMetrics", riskAnalyticsService.getCurrentRiskMetrics());
            exportData.put("portfolioAnalysis", riskAnalyticsService.getDetailedPortfolioRiskAnalysis());
            exportData.put("customerRiskData", riskAnalyticsService.getCustomerRiskDistribution());
            exportData.put("performanceTrends", riskAnalyticsService.getLoanPerformanceTrends());
            exportData.put("exportedAt", LocalDateTime.now());
            exportData.put("format", format);
            
            return ResponseEntity.ok(exportData);
            
        } catch (Exception e) {
            logger.error("Error exporting dashboard data", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to export data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // Utility method to trigger real-time updates
    public void broadcastDashboardUpdate(String updateType, Object data) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("updateType", updateType);
            update.put("data", data);
            update.put("timestamp", LocalDateTime.now());
            
            dashboardUpdatesSink.tryEmitNext(update);
            
        } catch (Exception e) {
            logger.error("Error broadcasting dashboard update", e);
        }
    }
}