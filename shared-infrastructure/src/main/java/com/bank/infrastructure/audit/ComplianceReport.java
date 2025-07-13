package com.bank.infrastructure.audit;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Compliance Report for Banking Regulations
 * 
 * Comprehensive reporting structure for regulatory compliance including:
 * - Compliance metrics and statistics
 * - Violation tracking and categorization
 * - Audit event summaries
 * - Executive dashboard data
 */
public class ComplianceReport {
    
    private final String regulation;
    private final LocalDateTime generatedAt;
    private final String reportId;
    
    private int totalEvents;
    private int compliantEvents;
    private final List<ComplianceViolation> violations;
    private final List<AuditEvent> events;
    private final Map<String, ComplianceMetric> metrics;
    private final Map<String, Object> executiveSummary;
    
    public ComplianceReport(String regulation) {
        this.regulation = regulation;
        this.generatedAt = LocalDateTime.now();
        this.reportId = UUID.randomUUID().toString();
        this.violations = new ArrayList<>();
        this.events = new ArrayList<>();
        this.metrics = new HashMap<>();
        this.executiveSummary = new HashMap<>();
        initializeMetrics();
    }
    
    /**
     * Initialize compliance metrics based on regulation
     */
    private void initializeMetrics() {
        switch (regulation) {
            case "SOX":
                metrics.put("financialReportingAccuracy", new ComplianceMetric("Financial Reporting Accuracy", "%"));
                metrics.put("internalControlEffectiveness", new ComplianceMetric("Internal Control Effectiveness", "%"));
                metrics.put("segregationOfDuties", new ComplianceMetric("Segregation of Duties", "score"));
                break;
                
            case "PCI-DSS":
                metrics.put("cardDataProtection", new ComplianceMetric("Card Data Protection Level", "%"));
                metrics.put("accessControlCompliance", new ComplianceMetric("Access Control Compliance", "%"));
                metrics.put("encryptionCompliance", new ComplianceMetric("Encryption Compliance", "%"));
                break;
                
            case "GDPR":
                metrics.put("dataPrivacyCompliance", new ComplianceMetric("Data Privacy Compliance", "%"));
                metrics.put("consentManagement", new ComplianceMetric("Consent Management", "%"));
                metrics.put("dataSubjectRequests", new ComplianceMetric("Data Subject Request Handling", "days"));
                break;
                
            case "Basel III":
                metrics.put("capitalAdequacyRatio", new ComplianceMetric("Capital Adequacy Ratio", "%"));
                metrics.put("liquidityCoverageRatio", new ComplianceMetric("Liquidity Coverage Ratio", "%"));
                metrics.put("leverageRatio", new ComplianceMetric("Leverage Ratio", "%"));
                break;
                
            case "AML":
                metrics.put("suspiciousActivityDetection", new ComplianceMetric("Suspicious Activity Detection Rate", "%"));
                metrics.put("kycCompleteness", new ComplianceMetric("KYC Completeness", "%"));
                metrics.put("sarFilingTimeliness", new ComplianceMetric("SAR Filing Timeliness", "hours"));
                break;
        }
    }
    
    /**
     * Add compliance violations
     */
    public void addViolations(List<ComplianceViolation> violations) {
        this.violations.addAll(violations);
    }
    
    /**
     * Add audit event to report
     */
    public void addEvent(AuditEvent event) {
        this.events.add(event);
    }
    
    /**
     * Increment event counters
     */
    public void incrementEventCount() {
        totalEvents++;
    }
    
    public void incrementCompliantCount() {
        compliantEvents++;
    }
    
    /**
     * Calculate compliance metrics
     */
    public void calculateMetrics() {
        // Overall compliance rate
        double complianceRate = totalEvents > 0 ? (double) compliantEvents / totalEvents * 100 : 100.0;
        
        // Regulation-specific calculations
        switch (regulation) {
            case "SOX":
                calculateSOXMetrics();
                break;
            case "PCI-DSS":
                calculatePCIDSSMetrics();
                break;
            case "GDPR":
                calculateGDPRMetrics();
                break;
            case "Basel III":
                calculateBaselIIIMetrics();
                break;
            case "AML":
                calculateAMLMetrics();
                break;
        }
        
        // Executive summary
        generateExecutiveSummary(complianceRate);
    }
    
    /**
     * Calculate SOX-specific metrics
     */
    private void calculateSOXMetrics() {
        // Financial reporting accuracy
        long accurateReports = events.stream()
            .filter(e -> e.getCategory() == AuditEvent.EventCategory.TRANSACTION)
            .filter(e -> e.getResult() == AuditEvent.ActionResult.SUCCESS)
            .count();
        
        double accuracy = events.isEmpty() ? 100.0 : 
            (double) accurateReports / events.size() * 100;
        
        metrics.get("financialReportingAccuracy").setValue(accuracy);
        
        // Internal control effectiveness
        long controlViolations = violations.stream()
            .filter(v -> v.getCode().startsWith("SOX_"))
            .count();
        
        double effectiveness = violations.isEmpty() ? 100.0 : 
            Math.max(0, 100.0 - (controlViolations * 10));
        
        metrics.get("internalControlEffectiveness").setValue(effectiveness);
        
        // Segregation of duties
        metrics.get("segregationOfDuties").setValue(calculateSegregationScore());
    }
    
    /**
     * Calculate PCI-DSS specific metrics
     */
    private void calculatePCIDSSMetrics() {
        // Card data protection
        long protectedAccess = events.stream()
            .filter(e -> e.getCategory() == AuditEvent.EventCategory.DATA_ACCESS)
            .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("encrypted"))
            .count();
        
        long totalCardAccess = events.stream()
            .filter(e -> e.getCategory() == AuditEvent.EventCategory.DATA_ACCESS)
            .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("cardData"))
            .count();
        
        double protection = totalCardAccess == 0 ? 100.0 : 
            (double) protectedAccess / totalCardAccess * 100;
        
        metrics.get("cardDataProtection").setValue(protection);
        
        // Access control compliance
        long unauthorizedAccess = violations.stream()
            .filter(v -> v.getCode().contains("ACCESS"))
            .count();
        
        double accessCompliance = Math.max(0, 100.0 - (unauthorizedAccess * 5));
        metrics.get("accessControlCompliance").setValue(accessCompliance);
        
        // Encryption compliance
        metrics.get("encryptionCompliance").setValue(calculateEncryptionCompliance());
    }
    
    /**
     * Calculate GDPR specific metrics
     */
    private void calculateGDPRMetrics() {
        // Data privacy compliance
        long privacyCompliant = events.stream()
            .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("gdprRelevant"))
            .filter(e -> e.getMetadata().containsKey("lawfulBasis"))
            .count();
        
        long gdprEvents = events.stream()
            .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("gdprRelevant"))
            .count();
        
        double privacyCompliance = gdprEvents == 0 ? 100.0 : 
            (double) privacyCompliant / gdprEvents * 100;
        
        metrics.get("dataPrivacyCompliance").setValue(privacyCompliance);
        
        // Consent management
        metrics.get("consentManagement").setValue(calculateConsentCompliance());
        
        // Data subject request handling time
        metrics.get("dataSubjectRequests").setValue(calculateAverageRequestTime());
    }
    
    /**
     * Calculate Basel III specific metrics
     */
    private void calculateBaselIIIMetrics() {
        // These would typically come from financial systems
        // Using mock calculations for demonstration
        
        // Capital Adequacy Ratio (CAR)
        double car = calculateCapitalAdequacyRatio();
        metrics.get("capitalAdequacyRatio").setValue(car);
        
        // Liquidity Coverage Ratio (LCR)
        double lcr = calculateLiquidityCoverageRatio();
        metrics.get("liquidityCoverageRatio").setValue(lcr);
        
        // Leverage Ratio
        double leverage = calculateLeverageRatio();
        metrics.get("leverageRatio").setValue(leverage);
    }
    
    /**
     * Calculate AML specific metrics
     */
    private void calculateAMLMetrics() {
        // Suspicious activity detection rate
        long suspiciousDetected = events.stream()
            .filter(e -> e.getCategory() == AuditEvent.EventCategory.FRAUD_DETECTION)
            .filter(e -> e.getMetadata() != null && e.getMetadata().containsKey("suspicious"))
            .count();
        
        long totalTransactions = events.stream()
            .filter(e -> e.getCategory() == AuditEvent.EventCategory.TRANSACTION)
            .count();
        
        double detectionRate = totalTransactions == 0 ? 0.0 : 
            (double) suspiciousDetected / totalTransactions * 100;
        
        metrics.get("suspiciousActivityDetection").setValue(detectionRate);
        
        // KYC completeness
        metrics.get("kycCompleteness").setValue(calculateKYCCompleteness());
        
        // SAR filing timeliness
        metrics.get("sarFilingTimeliness").setValue(calculateSARTimeliness());
    }
    
    /**
     * Generate executive summary
     */
    private void generateExecutiveSummary(double complianceRate) {
        executiveSummary.put("regulation", regulation);
        executiveSummary.put("reportingPeriod", "Last 30 days"); // Simplified
        executiveSummary.put("overallComplianceRate", String.format("%.2f%%", complianceRate));
        executiveSummary.put("totalEventsAnalyzed", totalEvents);
        executiveSummary.put("totalViolations", violations.size());
        
        // Critical violations
        long criticalViolations = violations.stream()
            .filter(v -> v.getSeverity() == ComplianceViolation.Severity.CRITICAL)
            .count();
        executiveSummary.put("criticalViolations", criticalViolations);
        
        // Top risks
        List<String> topRisks = identifyTopRisks();
        executiveSummary.put("topRisks", topRisks);
        
        // Recommendations
        List<String> recommendations = generateRecommendations();
        executiveSummary.put("recommendations", recommendations);
        
        // Compliance trend
        executiveSummary.put("complianceTrend", calculateComplianceTrend());
    }
    
    /**
     * Identify top compliance risks
     */
    private List<String> identifyTopRisks() {
        Map<String, Long> violationCounts = new HashMap<>();
        
        for (ComplianceViolation violation : violations) {
            String category = violation.getCode().split("_")[0];
            violationCounts.merge(category, 1L, Long::sum);
        }
        
        return violationCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(3)
            .map(entry -> entry.getKey() + " (" + entry.getValue() + " violations)")
            .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);
    }
    
    /**
     * Generate compliance recommendations
     */
    private List<String> generateRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        // Based on violations
        if (violations.stream().anyMatch(v -> v.getCode().contains("ACCESS"))) {
            recommendations.add("Strengthen access control policies and monitoring");
        }
        
        if (violations.stream().anyMatch(v -> v.getCode().contains("APPROVAL"))) {
            recommendations.add("Implement automated approval workflows for high-risk transactions");
        }
        
        if (violations.stream().anyMatch(v -> v.getSeverity() == ComplianceViolation.Severity.CRITICAL)) {
            recommendations.add("Immediate review required for critical compliance violations");
        }
        
        // Based on metrics
        for (ComplianceMetric metric : metrics.values()) {
            if (metric.getValue() < 80.0) {
                recommendations.add("Improve " + metric.getName() + " (currently at " + 
                                  String.format("%.1f", metric.getValue()) + metric.getUnit() + ")");
            }
        }
        
        return recommendations;
    }
    
    // Mock calculation methods (would be replaced with actual calculations in production)
    private double calculateSegregationScore() { return 85.0; }
    private double calculateEncryptionCompliance() { return 95.0; }
    private double calculateConsentCompliance() { return 88.0; }
    private double calculateAverageRequestTime() { return 2.5; }
    private double calculateCapitalAdequacyRatio() { return 15.2; }
    private double calculateLiquidityCoverageRatio() { return 125.0; }
    private double calculateLeverageRatio() { return 4.8; }
    private double calculateKYCCompleteness() { return 92.0; }
    private double calculateSARTimeliness() { return 18.5; }
    private String calculateComplianceTrend() { return "IMPROVING"; }
    
    // Getters
    public String getRegulation() { return regulation; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getReportId() { return reportId; }
    public List<ComplianceViolation> getViolations() { return new ArrayList<>(violations); }
    public Map<String, ComplianceMetric> getMetrics() { return new HashMap<>(metrics); }
    public Map<String, Object> getExecutiveSummary() { return new HashMap<>(executiveSummary); }
    
    /**
     * Generate formatted report
     */
    public String generateFormattedReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== COMPLIANCE REPORT ===\n");
        report.append("Regulation: ").append(regulation).append("\n");
        report.append("Generated: ").append(generatedAt).append("\n");
        report.append("Report ID: ").append(reportId).append("\n\n");
        
        report.append("=== EXECUTIVE SUMMARY ===\n");
        executiveSummary.forEach((key, value) -> 
            report.append(key).append(": ").append(value).append("\n"));
        
        report.append("\n=== COMPLIANCE METRICS ===\n");
        metrics.forEach((key, metric) -> 
            report.append(metric.getName()).append(": ")
                  .append(String.format("%.2f", metric.getValue()))
                  .append(metric.getUnit()).append("\n"));
        
        if (!violations.isEmpty()) {
            report.append("\n=== VIOLATIONS ===\n");
            violations.forEach(violation -> 
                report.append(violation.toString()).append("\n"));
        }
        
        return report.toString();
    }
}