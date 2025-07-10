package com.loanmanagement.performance;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Performance Optimization Report Generator
 * Analyzes performance test results and provides optimization recommendations
 */
@Slf4j
@Getter
public class PerformanceOptimizationReport {
    
    private final PerformanceMetricsCollector.PerformanceReport performanceReport;
    private final List<OptimizationRecommendation> recommendations;
    private final PerformanceAnalysis analysis;
    
    public PerformanceOptimizationReport(PerformanceMetricsCollector.PerformanceReport performanceReport) {
        this.performanceReport = performanceReport;
        this.analysis = analyzePerformance();
        this.recommendations = generateRecommendations();
    }
    
    /**
     * Generate comprehensive optimization report
     */
    public String generateOptimizationReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("PERFORMANCE OPTIMIZATION REPORT\n");
        report.append("===============================\n\n");
        
        report.append("Test Overview:\n");
        report.append(String.format("- Test Duration: %d seconds\n", performanceReport.testDuration().toSeconds()));
        report.append(String.format("- Total Requests: %d\n", performanceReport.overallMetrics().totalRequests()));
        report.append(String.format("- Overall Success Rate: %.2f%%\n", performanceReport.overallMetrics().overallSuccessRate()));
        report.append(String.format("- Overall Throughput: %.2f req/sec\n\n", performanceReport.overallMetrics().throughput()));
        
        report.append("Performance Analysis:\n");
        report.append(analysis.generateAnalysisReport());
        report.append("\n");
        
        report.append("Optimization Recommendations:\n");
        report.append("=============================\n");
        
        for (int i = 0; i < recommendations.size(); i++) {
            OptimizationRecommendation rec = recommendations.get(i);
            report.append(String.format("%d. %s (Priority: %s)\n", i + 1, rec.title(), rec.priority()));
            report.append(String.format("   Issue: %s\n", rec.issue()));
            report.append(String.format("   Solution: %s\n", rec.solution()));
            report.append(String.format("   Expected Impact: %s\n", rec.expectedImpact()));
            if (!rec.implementationSteps().isEmpty()) {
                report.append("   Implementation Steps:\n");
                for (String step : rec.implementationSteps()) {
                    report.append(String.format("   - %s\n", step));
                }
            }
            report.append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Analyze performance metrics to identify issues
     */
    private PerformanceAnalysis analyzePerformance() {
        List<PerformanceIssue> issues = new ArrayList<>();
        Map<String, String> metrics = new HashMap<>();
        
        // Analyze overall performance
        if (performanceReport.overallMetrics().overallSuccessRate() < 95.0) {
            issues.add(new PerformanceIssue(
                    IssueSeverity.HIGH,
                    "Low Success Rate",
                    String.format("Success rate is %.2f%%, below acceptable threshold of 95%%", 
                            performanceReport.overallMetrics().overallSuccessRate())
            ));
        }
        
        if (performanceReport.overallMetrics().throughput() < 50) {
            issues.add(new PerformanceIssue(
                    IssueSeverity.MEDIUM,
                    "Low Throughput",
                    String.format("Throughput is %.2f req/sec, could be improved", 
                            performanceReport.overallMetrics().throughput())
            ));
        }
        
        // Analyze individual operations
        for (Map.Entry<String, PerformanceMetricsCollector.OperationMetrics> entry : 
                performanceReport.operationMetrics().entrySet()) {
            
            String operation = entry.getKey();
            PerformanceMetricsCollector.OperationMetrics operationMetrics = entry.getValue();
            
            // Check response times
            if (operationMetrics.avgResponseTime() > getAcceptableResponseTime(operation)) {
                issues.add(new PerformanceIssue(
                        IssueSeverity.MEDIUM,
                        "High Response Time",
                        String.format("%s has average response time of %d ms", 
                                operation, operationMetrics.avgResponseTime())
                ));
            }
            
            // Check P95 response times
            if (operationMetrics.p95ResponseTime() > getAcceptableResponseTime(operation) * 2) {
                issues.add(new PerformanceIssue(
                        IssueSeverity.HIGH,
                        "High P95 Response Time",
                        String.format("%s P95 response time is %d ms", 
                                operation, operationMetrics.p95ResponseTime())
                ));
            }
            
            // Check error rates
            if (operationMetrics.errorRate() > 5.0) {
                issues.add(new PerformanceIssue(
                        IssueSeverity.HIGH,
                        "High Error Rate",
                        String.format("%s has error rate of %.2f%%", 
                                operation, operationMetrics.errorRate())
                ));
            }
            
            // Store metrics for analysis
            metrics.put(operation + "_avg_response", String.valueOf(operationMetrics.avgResponseTime()));
            metrics.put(operation + "_p95_response", String.valueOf(operationMetrics.p95ResponseTime()));
            metrics.put(operation + "_error_rate", String.valueOf(operationMetrics.errorRate()));
        }
        
        return new PerformanceAnalysis(issues, metrics, identifyBottlenecks(), suggestOptimizations());
    }
    
    /**
     * Generate optimization recommendations based on analysis
     */
    private List<OptimizationRecommendation> generateRecommendations() {
        List<OptimizationRecommendation> recommendations = new ArrayList<>();
        
        // Database optimization recommendations
        if (hasSlowDatabaseOperations()) {
            recommendations.add(new OptimizationRecommendation(
                    "Database Query Optimization",
                    RecommendationPriority.HIGH,
                    "Database operations are slow, affecting overall performance",
                    "Optimize database queries, add appropriate indexes, and consider connection pooling adjustments",
                    "30-50% improvement in response times for database-heavy operations",
                    Arrays.asList(
                            "Analyze slow query logs",
                            "Add indexes for frequently queried columns",
                            "Optimize N+1 query problems",
                            "Increase database connection pool size",
                            "Consider read replicas for read-heavy operations"
                    )
            ));
        }
        
        // Caching recommendations
        if (hasCacheableOperations()) {
            recommendations.add(new OptimizationRecommendation(
                    "Implement Strategic Caching",
                    RecommendationPriority.HIGH,
                    "Frequently accessed data is being retrieved from database repeatedly",
                    "Implement Redis caching for frequently accessed customer and loan data",
                    "40-60% improvement in read operation response times",
                    Arrays.asList(
                            "Cache customer lookup operations",
                            "Cache loan details with appropriate TTL",
                            "Implement cache-aside pattern",
                            "Add cache warming for critical data",
                            "Monitor cache hit ratios"
                    )
            ));
        }
        
        // API optimization recommendations
        if (hasSlowApiOperations()) {
            recommendations.add(new OptimizationRecommendation(
                    "API Response Optimization",
                    RecommendationPriority.MEDIUM,
                    "API endpoints have high response times",
                    "Optimize serialization, reduce payload sizes, and implement async processing",
                    "20-30% improvement in API response times",
                    Arrays.asList(
                            "Implement pagination for list endpoints",
                            "Use DTOs to reduce payload sizes",
                            "Implement async processing for heavy operations",
                            "Add response compression",
                            "Optimize JSON serialization"
                    )
            ));
        }
        
        // Infrastructure recommendations
        if (hasResourceConstraints()) {
            recommendations.add(new OptimizationRecommendation(
                    "Infrastructure Scaling",
                    RecommendationPriority.MEDIUM,
                    "System resources appear to be constrained under load",
                    "Scale application instances and optimize resource allocation",
                    "Improved ability to handle concurrent load",
                    Arrays.asList(
                            "Increase application server instances",
                            "Optimize JVM heap settings",
                            "Configure load balancer properly",
                            "Monitor CPU and memory usage",
                            "Consider containerization improvements"
                    )
            ));
        }
        
        // Concurrency optimization
        if (hasConcurrencyIssues()) {
            recommendations.add(new OptimizationRecommendation(
                    "Concurrency Optimization",
                    RecommendationPriority.HIGH,
                    "Performance degrades significantly under concurrent load",
                    "Optimize thread pools, connection pools, and synchronization",
                    "Improved scalability and reduced response time variance",
                    Arrays.asList(
                            "Optimize thread pool configurations",
                            "Review database connection pool settings",
                            "Minimize synchronized blocks",
                            "Implement proper error handling",
                            "Use asynchronous processing where appropriate"
                    )
            ));
        }
        
        // Sort by priority
        return recommendations.stream()
                .sorted((r1, r2) -> r1.priority().compareTo(r2.priority()))
                .collect(Collectors.toList());
    }
    
    // Helper methods for analysis
    
    private long getAcceptableResponseTime(String operation) {
        return switch (operation.toLowerCase()) {
            case "createcustomer", "createloan" -> 1000L; // 1 second for creation operations
            case "getcustomer", "getloan" -> 500L; // 500ms for read operations
            case "processpayment" -> 1500L; // 1.5 seconds for payment processing
            default -> 1000L; // Default 1 second
        };
    }
    
    private boolean hasSlowDatabaseOperations() {
        return performanceReport.operationMetrics().values().stream()
                .anyMatch(metrics -> metrics.operation().toLowerCase().contains("repository") 
                        && metrics.avgResponseTime() > 200);
    }
    
    private boolean hasCacheableOperations() {
        return performanceReport.operationMetrics().values().stream()
                .anyMatch(metrics -> metrics.operation().toLowerCase().contains("get") 
                        && metrics.totalRequests() > 100);
    }
    
    private boolean hasSlowApiOperations() {
        return performanceReport.operationMetrics().values().stream()
                .anyMatch(metrics -> metrics.operation().toLowerCase().contains("api") 
                        && metrics.avgResponseTime() > 1000);
    }
    
    private boolean hasResourceConstraints() {
        return performanceReport.overallMetrics().throughput() < 30 
                || performanceReport.operationMetrics().values().stream()
                .anyMatch(metrics -> metrics.p95ResponseTime() > metrics.avgResponseTime() * 3);
    }
    
    private boolean hasConcurrencyIssues() {
        return performanceReport.operationMetrics().values().stream()
                .anyMatch(metrics -> metrics.errorRate() > 5.0 
                        || metrics.p99ResponseTime() > metrics.avgResponseTime() * 5);
    }
    
    private List<String> identifyBottlenecks() {
        List<String> bottlenecks = new ArrayList<>();
        
        // Find slowest operations
        performanceReport.operationMetrics().values().stream()
                .filter(metrics -> metrics.avgResponseTime() > getAcceptableResponseTime(metrics.operation()))
                .sorted((m1, m2) -> Long.compare(m2.avgResponseTime(), m1.avgResponseTime()))
                .limit(3)
                .forEach(metrics -> bottlenecks.add(
                        String.format("%s (avg: %dms)", metrics.operation(), metrics.avgResponseTime())));
        
        // Check for high error rates
        performanceReport.operationMetrics().values().stream()
                .filter(metrics -> metrics.errorRate() > 5.0)
                .forEach(metrics -> bottlenecks.add(
                        String.format("%s (%.2f%% error rate)", metrics.operation(), metrics.errorRate())));
        
        return bottlenecks;
    }
    
    private List<String> suggestOptimizations() {
        List<String> optimizations = new ArrayList<>();
        
        optimizations.add("Enable database query logging to identify slow queries");
        optimizations.add("Implement application-level caching for frequently accessed data");
        optimizations.add("Consider using connection pooling for external services");
        optimizations.add("Optimize serialization/deserialization processes");
        optimizations.add("Implement proper error handling and circuit breakers");
        
        return optimizations;
    }
    
    // Data classes
    
    public record PerformanceAnalysis(
            List<PerformanceIssue> issues,
            Map<String, String> metrics,
            List<String> bottlenecks,
            List<String> optimizations
    ) {
        public String generateAnalysisReport() {
            StringBuilder report = new StringBuilder();
            
            report.append("Issues Identified:\n");
            if (issues.isEmpty()) {
                report.append("- No significant performance issues detected\n");
            } else {
                for (PerformanceIssue issue : issues) {
                    report.append(String.format("- %s (%s): %s\n", 
                            issue.title(), issue.severity(), issue.description()));
                }
            }
            
            report.append("\nBottlenecks:\n");
            if (bottlenecks.isEmpty()) {
                report.append("- No major bottlenecks identified\n");
            } else {
                for (String bottleneck : bottlenecks) {
                    report.append(String.format("- %s\n", bottleneck));
                }
            }
            
            return report.toString();
        }
    }
    
    public record PerformanceIssue(
            IssueSeverity severity,
            String title,
            String description
    ) {}
    
    public record OptimizationRecommendation(
            String title,
            RecommendationPriority priority,
            String issue,
            String solution,
            String expectedImpact,
            List<String> implementationSteps
    ) {}
    
    public enum IssueSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    public enum RecommendationPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}