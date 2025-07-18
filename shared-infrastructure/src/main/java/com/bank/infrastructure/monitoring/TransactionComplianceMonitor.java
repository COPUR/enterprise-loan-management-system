package com.bank.infrastructure.monitoring;

import com.bank.infrastructure.domain.Money;
import com.bank.infrastructure.security.BankingComplianceFramework;
import com.bank.infrastructure.security.BankingComplianceFramework.ComplianceStandard;
import com.bank.infrastructure.pattern.BankingPatternMatching;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Transaction Compliance Monitor for Real-time Violation Detection
 * 
 * Comprehensive transaction monitoring system providing:
 * - Real-time compliance violation detection
 * - Automated regulatory reporting
 * - Transaction pattern analysis for suspicious activity
 * - AML/KYC continuous monitoring
 * - Islamic banking Sharia compliance verification
 * - Large transaction reporting (CTR/SAR)
 * - Cross-border transaction compliance
 * - Rate limiting and velocity checks
 * 
 * Integrates with multiple regulatory frameworks and provides
 * automated response to compliance violations.
 */
@Component
public class TransactionComplianceMonitor {

    @Autowired
    private BankingComplianceFramework complianceFramework;
    
    @Autowired
    private BankingPatternMatching patternMatching;

    // Monitoring metrics
    private final AtomicLong totalTransactions = new AtomicLong(0);
    private final AtomicLong violationCount = new AtomicLong(0);
    private final AtomicLong amlAlerts = new AtomicLong(0);
    private final AtomicLong largeTransactionReports = new AtomicLong(0);
    private final AtomicLong blockedTransactions = new AtomicLong(0);

    // Transaction tracking
    private final Map<String, List<TransactionEvent>> customerTransactionHistory = new ConcurrentHashMap<>();
    private final Map<String, ComplianceViolation> activeViolations = new ConcurrentHashMap<>();
    private final Set<String> blockedCustomers = ConcurrentHashMap.newKeySet();
    private final Map<String, TransactionLimits> customerLimits = new ConcurrentHashMap<>();
    private final Map<String, RiskProfile> customerRiskProfiles = new ConcurrentHashMap<>();

    // Monitoring thresholds
    private static final BigDecimal CTR_THRESHOLD = BigDecimal.valueOf(10000); // Currency Transaction Report
    private static final BigDecimal SAR_THRESHOLD = BigDecimal.valueOf(5000);  // Suspicious Activity Report
    private static final int MAX_DAILY_TRANSACTIONS = 50;
    private static final BigDecimal DAILY_LIMIT = BigDecimal.valueOf(100000);
    private static final Duration VELOCITY_WINDOW = Duration.ofMinutes(10);

    /**
     * Transaction event record
     */
    public record TransactionEvent(
        String transactionId,
        String customerId,
        Money amount,
        TransactionType type,
        String counterparty,
        String description,
        Instant timestamp,
        Map<String, Object> metadata,
        TransactionStatus status
    ) {}

    /**
     * Transaction types
     */
    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, LOAN_PAYMENT, INVESTMENT,
        FOREIGN_EXCHANGE, WIRE_TRANSFER, ACH, CARD_PAYMENT, 
        ISLAMIC_MURABAHA, ISLAMIC_IJARA, ISLAMIC_MUSHARAKA
    }

    /**
     * Transaction status
     */
    public enum TransactionStatus {
        PENDING, APPROVED, REJECTED, UNDER_REVIEW, BLOCKED
    }

    /**
     * Compliance violation record
     */
    public record ComplianceViolation(
        String violationId,
        String customerId,
        String transactionId,
        ViolationType violationType,
        ComplianceStandard standard,
        String description,
        ViolationSeverity severity,
        Instant detectedAt,
        ViolationStatus status,
        Map<String, Object> context
    ) {}

    /**
     * Violation types
     */
    public enum ViolationType {
        LARGE_CASH_TRANSACTION, SUSPICIOUS_PATTERN, VELOCITY_LIMIT_EXCEEDED,
        DAILY_LIMIT_EXCEEDED, AML_WATCHLIST_MATCH, SANCTIONS_VIOLATION,
        SHARIA_NON_COMPLIANCE, PEP_TRANSACTION, STRUCTURING_PATTERN,
        CROSS_BORDER_VIOLATION, KYC_INCOMPLETE, UNUSUAL_ACTIVITY
    }

    /**
     * Violation severity
     */
    public enum ViolationSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Violation status
     */
    public enum ViolationStatus {
        OPEN, INVESTIGATING, REPORTED, RESOLVED, FALSE_POSITIVE
    }

    /**
     * Customer risk profile
     */
    public record RiskProfile(
        String customerId,
        RiskLevel riskLevel,
        BigDecimal riskScore,
        Map<String, Object> riskFactors,
        Instant lastUpdated,
        List<String> watchlistFlags
    ) {}

    /**
     * Risk levels
     */
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH, PROHIBITED
    }

    /**
     * Transaction limits
     */
    public record TransactionLimits(
        String customerId,
        BigDecimal dailyLimit,
        BigDecimal transactionLimit,
        int maxDailyTransactions,
        Map<TransactionType, BigDecimal> typeSpecificLimits,
        Instant lastUpdated
    ) {}

    /**
     * Monitor incoming transaction for compliance
     */
    public ComplianceMonitoringResult monitorTransaction(TransactionEvent transaction) {
        totalTransactions.incrementAndGet();
        
        List<ComplianceViolation> violations = new ArrayList<>();
        List<String> alerts = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        try {
            // Store transaction event
            recordTransactionEvent(transaction);
            
            // Check if customer is blocked
            if (blockedCustomers.contains(transaction.customerId())) {
                violations.add(createViolation(transaction, ViolationType.UNUSUAL_ACTIVITY, 
                    ComplianceStandard.AML, "Transaction from blocked customer", ViolationSeverity.CRITICAL));
                blockedTransactions.incrementAndGet();
                return new ComplianceMonitoringResult(transaction.transactionId(), false, violations, alerts, recommendations);
            }
            
            // Large transaction reporting (CTR)
            if (transaction.amount().getAmount().compareTo(CTR_THRESHOLD) > 0) {
                alerts.add("Large transaction requires CTR filing");
                largeTransactionReports.incrementAndGet();
                generateCTRReport(transaction);
            }
            
            // Suspicious activity detection (SAR)
            if (detectSuspiciousActivity(transaction)) {
                violations.add(createViolation(transaction, ViolationType.SUSPICIOUS_PATTERN,
                    ComplianceStandard.AML, "Suspicious activity pattern detected", ViolationSeverity.HIGH));
                amlAlerts.incrementAndGet();
                generateSARReport(transaction);
            }
            
            // Velocity and frequency checks
            if (checkVelocityViolation(transaction)) {
                violations.add(createViolation(transaction, ViolationType.VELOCITY_LIMIT_EXCEEDED,
                    ComplianceStandard.AML, "Transaction velocity limit exceeded", ViolationSeverity.MEDIUM));
            }
            
            // Daily limits check
            if (checkDailyLimits(transaction)) {
                violations.add(createViolation(transaction, ViolationType.DAILY_LIMIT_EXCEEDED,
                    ComplianceStandard.AML, "Daily transaction limit exceeded", ViolationSeverity.MEDIUM));
            }
            
            // AML watchlist screening
            if (checkAMLWatchlist(transaction)) {
                violations.add(createViolation(transaction, ViolationType.AML_WATCHLIST_MATCH,
                    ComplianceStandard.AML, "Customer matches AML watchlist", ViolationSeverity.CRITICAL));
            }
            
            // Sanctions screening
            if (checkSanctionsViolation(transaction)) {
                violations.add(createViolation(transaction, ViolationType.SANCTIONS_VIOLATION,
                    ComplianceStandard.AML, "Transaction violates sanctions regulations", ViolationSeverity.CRITICAL));
            }
            
            // Islamic banking compliance (for Islamic transactions)
            if (isIslamicTransaction(transaction) && checkShariaCompliance(transaction)) {
                violations.add(createViolation(transaction, ViolationType.SHARIA_NON_COMPLIANCE,
                    ComplianceStandard.SHARIA, "Transaction violates Sharia principles", ViolationSeverity.HIGH));
            }
            
            // Cross-border transaction compliance
            if (isCrossBorderTransaction(transaction) && checkCrossBorderCompliance(transaction)) {
                violations.add(createViolation(transaction, ViolationType.CROSS_BORDER_VIOLATION,
                    ComplianceStandard.AML, "Cross-border transaction compliance violation", ViolationSeverity.HIGH));
            }
            
            // Structuring pattern detection
            if (detectStructuring(transaction)) {
                violations.add(createViolation(transaction, ViolationType.STRUCTURING_PATTERN,
                    ComplianceStandard.AML, "Potential structuring pattern detected", ViolationSeverity.HIGH));
            }
            
            // Update customer risk profile
            updateCustomerRiskProfile(transaction, violations);
            
            // Process violations
            if (!violations.isEmpty()) {
                violationCount.addAndGet(violations.size());
                processViolations(violations);
            }
            
            // Generate recommendations
            recommendations.addAll(generateRecommendations(transaction, violations));
            
            boolean approved = violations.stream().noneMatch(v -> v.severity() == ViolationSeverity.CRITICAL);
            
            return new ComplianceMonitoringResult(transaction.transactionId(), approved, violations, alerts, recommendations);
            
        } catch (Exception e) {
            System.err.println("Error monitoring transaction: " + transaction.transactionId() + " - " + e.getMessage());
            return new ComplianceMonitoringResult(transaction.transactionId(), false, 
                List.of(createViolation(transaction, ViolationType.UNUSUAL_ACTIVITY, ComplianceStandard.AML, 
                       "Monitoring system error", ViolationSeverity.HIGH)), 
                List.of("System error during monitoring"), List.of());
        }
    }

    /**
     * Record transaction event for analysis
     */
    private void recordTransactionEvent(TransactionEvent transaction) {
        customerTransactionHistory.computeIfAbsent(transaction.customerId(), k -> new ArrayList<>())
                                 .add(transaction);
        
        // Keep only recent transactions (last 90 days)
        Instant cutoff = Instant.now().minus(Duration.ofDays(90));
        customerTransactionHistory.get(transaction.customerId())
                                  .removeIf(event -> event.timestamp().isBefore(cutoff));
    }

    /**
     * Detect suspicious activity patterns
     */
    private boolean detectSuspiciousActivity(TransactionEvent transaction) {
        List<TransactionEvent> history = customerTransactionHistory.get(transaction.customerId());
        if (history == null || history.size() < 2) {
            return false;
        }
        
        // Check for round number transactions (potential structuring)
        if (isRoundNumber(transaction.amount().getAmount())) {
            long recentRoundTransactions = history.stream()
                .filter(t -> t.timestamp().isAfter(Instant.now().minus(Duration.ofDays(7))))
                .filter(t -> isRoundNumber(t.amount().getAmount()))
                .count();
            
            if (recentRoundTransactions > 5) {
                return true;
            }
        }
        
        // Check for unusual transaction times
        int hour = LocalDateTime.now().getHour();
        if (hour < 6 || hour > 22) {
            BigDecimal avgAmount = calculateAverageAmount(history);
            if (transaction.amount().getAmount().compareTo(avgAmount.multiply(BigDecimal.valueOf(3))) > 0) {
                return true;
            }
        }
        
        // Check for geographic anomalies (simplified)
        if (transaction.metadata().containsKey("location")) {
            String location = (String) transaction.metadata().get("location");
            if (isUnusualLocation(history, location)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Check velocity violations
     */
    private boolean checkVelocityViolation(TransactionEvent transaction) {
        List<TransactionEvent> history = customerTransactionHistory.get(transaction.customerId());
        if (history == null) {
            return false;
        }
        
        // Check transactions in the last 10 minutes
        Instant windowStart = Instant.now().minus(VELOCITY_WINDOW);
        List<TransactionEvent> recentTransactions = history.stream()
            .filter(t -> t.timestamp().isAfter(windowStart))
            .collect(Collectors.toList());
        
        // Velocity check: more than 5 transactions in 10 minutes
        if (recentTransactions.size() > 5) {
            return true;
        }
        
        // Amount velocity check
        BigDecimal totalAmount = recentTransactions.stream()
            .map(t -> t.amount().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalAmount.compareTo(BigDecimal.valueOf(50000)) > 0;
    }

    /**
     * Check daily limits
     */
    private boolean checkDailyLimits(TransactionEvent transaction) {
        List<TransactionEvent> history = customerTransactionHistory.get(transaction.customerId());
        if (history == null) {
            return false;
        }
        
        // Get today's transactions
        Instant startOfDay = Instant.now().minus(Duration.ofDays(1));
        List<TransactionEvent> todayTransactions = history.stream()
            .filter(t -> t.timestamp().isAfter(startOfDay))
            .collect(Collectors.toList());
        
        // Check transaction count
        if (todayTransactions.size() >= MAX_DAILY_TRANSACTIONS) {
            return true;
        }
        
        // Check total amount
        BigDecimal totalAmount = todayTransactions.stream()
            .map(t -> t.amount().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        TransactionLimits limits = customerLimits.get(transaction.customerId());
        BigDecimal dailyLimit = limits != null ? limits.dailyLimit() : DAILY_LIMIT;
        
        return totalAmount.add(transaction.amount().getAmount()).compareTo(dailyLimit) > 0;
    }

    /**
     * Detect structuring patterns
     */
    private boolean detectStructuring(TransactionEvent transaction) {
        List<TransactionEvent> history = customerTransactionHistory.get(transaction.customerId());
        if (history == null || history.size() < 3) {
            return false;
        }
        
        // Look for multiple transactions just under CTR threshold
        Instant windowStart = Instant.now().minus(Duration.ofDays(7));
        List<TransactionEvent> recentTransactions = history.stream()
            .filter(t -> t.timestamp().isAfter(windowStart))
            .filter(t -> t.amount().getAmount().compareTo(BigDecimal.valueOf(9000)) > 0)
            .filter(t -> t.amount().getAmount().compareTo(CTR_THRESHOLD) < 0)
            .collect(Collectors.toList());
        
        // Multiple transactions just under threshold indicates potential structuring
        return recentTransactions.size() >= 3;
    }

    /**
     * Generate CTR (Currency Transaction Report)
     */
    private void generateCTRReport(TransactionEvent transaction) {
        CTRReport report = new CTRReport(
            UUID.randomUUID().toString(),
            transaction.customerId(),
            transaction.transactionId(),
            transaction.amount(),
            transaction.type(),
            Instant.now(),
            "AUTO_GENERATED",
            Map.of("threshold", CTR_THRESHOLD.toString(), "automated", "true")
        );
        
        // In production, send to regulatory authorities
        System.out.println("CTR Report Generated: " + report);
    }

    /**
     * Generate SAR (Suspicious Activity Report)
     */
    private void generateSARReport(TransactionEvent transaction) {
        SARReport report = new SARReport(
            UUID.randomUUID().toString(),
            transaction.customerId(),
            transaction.transactionId(),
            "Suspicious transaction pattern detected",
            SuspiciousActivityType.UNUSUAL_TRANSACTION_PATTERN,
            Instant.now(),
            "AUTO_GENERATED",
            Map.of("automated", "true", "pattern", "velocity_anomaly")
        );
        
        // In production, send to FinCEN
        System.out.println("SAR Report Generated: " + report);
    }

    /**
     * Process compliance violations
     */
    private void processViolations(List<ComplianceViolation> violations) {
        for (ComplianceViolation violation : violations) {
            activeViolations.put(violation.violationId(), violation);
            
            // Take automated action based on severity
            switch (violation.severity()) {
                case CRITICAL -> {
                    // Block customer immediately
                    blockedCustomers.add(violation.customerId());
                    notifyComplianceTeam(violation);
                }
                case HIGH -> {
                    // Require manual review
                    flagForReview(violation);
                    notifyComplianceTeam(violation);
                }
                case MEDIUM -> {
                    // Enhanced monitoring
                    enhanceMonitoring(violation.customerId());
                }
                case LOW -> {
                    // Log for analysis
                    logViolation(violation);
                }
            }
        }
    }

    /**
     * Generate compliance recommendations
     */
    private List<String> generateRecommendations(TransactionEvent transaction, List<ComplianceViolation> violations) {
        List<String> recommendations = new ArrayList<>();
        
        if (violations.isEmpty()) {
            recommendations.add("Transaction approved - no compliance issues detected");
            return recommendations;
        }
        
        for (ComplianceViolation violation : violations) {
            switch (violation.violationType()) {
                case LARGE_CASH_TRANSACTION -> 
                    recommendations.add("File CTR report with regulatory authorities");
                case SUSPICIOUS_PATTERN -> 
                    recommendations.add("Consider filing SAR report and enhance customer monitoring");
                case VELOCITY_LIMIT_EXCEEDED -> 
                    recommendations.add("Implement velocity controls and review customer limits");
                case AML_WATCHLIST_MATCH -> 
                    recommendations.add("Immediate enhanced due diligence required");
                case SANCTIONS_VIOLATION -> 
                    recommendations.add("Block transaction and report to OFAC immediately");
                case SHARIA_NON_COMPLIANCE -> 
                    recommendations.add("Review with Sharia board and provide alternative Islamic product");
                default -> 
                    recommendations.add("Review transaction manually and update compliance procedures");
            }
        }
        
        return recommendations;
    }

    /**
     * Update customer risk profile based on transaction activity
     */
    private void updateCustomerRiskProfile(TransactionEvent transaction, List<ComplianceViolation> violations) {
        RiskProfile currentProfile = customerRiskProfiles.get(transaction.customerId());
        
        BigDecimal riskScore = currentProfile != null ? currentProfile.riskScore() : BigDecimal.valueOf(50);
        
        // Adjust risk score based on violations
        for (ComplianceViolation violation : violations) {
            switch (violation.severity()) {
                case CRITICAL -> riskScore = riskScore.add(BigDecimal.valueOf(30));
                case HIGH -> riskScore = riskScore.add(BigDecimal.valueOf(20));
                case MEDIUM -> riskScore = riskScore.add(BigDecimal.valueOf(10));
                case LOW -> riskScore = riskScore.add(BigDecimal.valueOf(5));
            }
        }
        
        // Cap risk score at 100
        riskScore = riskScore.min(BigDecimal.valueOf(100));
        
        RiskLevel riskLevel = determineRiskLevel(riskScore);
        
        RiskProfile updatedProfile = new RiskProfile(
            transaction.customerId(),
            riskLevel,
            riskScore,
            Map.of("lastTransactionAmount", transaction.amount().getAmount().toString(),
                   "violationCount", String.valueOf(violations.size())),
            Instant.now(),
            new ArrayList<>()
        );
        
        customerRiskProfiles.put(transaction.customerId(), updatedProfile);
    }

    /**
     * Scheduled compliance monitoring tasks
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void performScheduledMonitoring() {
        // Clean up old transaction history
        cleanupOldTransactions();
        
        // Analyze patterns for all customers
        analyzeCustomerPatterns();
        
        // Generate compliance reports
        generateComplianceReports();
        
        // Update risk profiles
        updateRiskProfiles();
    }

    /**
     * Get monitoring dashboard
     */
    public ComplianceMonitoringDashboard getMonitoringDashboard() {
        Map<ViolationType, Long> violationsByType = activeViolations.values().stream()
            .collect(Collectors.groupingBy(ComplianceViolation::violationType, Collectors.counting()));
            
        Map<ViolationSeverity, Long> violationsBySeverity = activeViolations.values().stream()
            .collect(Collectors.groupingBy(ComplianceViolation::severity, Collectors.counting()));
            
        long highRiskCustomers = customerRiskProfiles.values().stream()
            .mapToLong(profile -> profile.riskLevel() == RiskLevel.HIGH || 
                                  profile.riskLevel() == RiskLevel.VERY_HIGH ? 1 : 0)
            .sum();
        
        return new ComplianceMonitoringDashboard(
            totalTransactions.get(),
            violationCount.get(),
            amlAlerts.get(),
            largeTransactionReports.get(),
            blockedTransactions.get(),
            blockedCustomers.size(),
            activeViolations.size(),
            highRiskCustomers,
            violationsByType,
            violationsBySeverity
        );
    }

    // Helper methods
    private ComplianceViolation createViolation(TransactionEvent transaction, ViolationType type,
                                               ComplianceStandard standard, String description, 
                                               ViolationSeverity severity) {
        return new ComplianceViolation(
            UUID.randomUUID().toString(),
            transaction.customerId(),
            transaction.transactionId(),
            type,
            standard,
            description,
            severity,
            Instant.now(),
            ViolationStatus.OPEN,
            Map.of("amount", transaction.amount().getAmount().toString(),
                   "type", transaction.type().toString())
        );
    }

    private boolean checkAMLWatchlist(TransactionEvent transaction) {
        // Simplified watchlist check
        return false; // In production, check against real AML watchlists
    }

    private boolean checkSanctionsViolation(TransactionEvent transaction) {
        // Simplified sanctions check
        return false; // In production, check against OFAC and other sanctions lists
    }

    private boolean checkShariaCompliance(TransactionEvent transaction) {
        return !isIslamicTransaction(transaction); // Simplified check
    }

    private boolean checkCrossBorderCompliance(TransactionEvent transaction) {
        return false; // Simplified check
    }

    private boolean isIslamicTransaction(TransactionEvent transaction) {
        return transaction.type().toString().startsWith("ISLAMIC_");
    }

    private boolean isCrossBorderTransaction(TransactionEvent transaction) {
        return transaction.metadata().containsKey("crossBorder") && 
               Boolean.TRUE.equals(transaction.metadata().get("crossBorder"));
    }

    private boolean isRoundNumber(BigDecimal amount) {
        return amount.remainder(BigDecimal.valueOf(1000)).equals(BigDecimal.ZERO);
    }

    private BigDecimal calculateAverageAmount(List<TransactionEvent> history) {
        return history.stream()
            .map(t -> t.amount().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(history.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private boolean isUnusualLocation(List<TransactionEvent> history, String location) {
        // Simplified location analysis
        return false;
    }

    private RiskLevel determineRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(80)) >= 0) return RiskLevel.VERY_HIGH;
        if (riskScore.compareTo(BigDecimal.valueOf(60)) >= 0) return RiskLevel.HIGH;
        if (riskScore.compareTo(BigDecimal.valueOf(40)) >= 0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private void cleanupOldTransactions() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(90));
        customerTransactionHistory.values().forEach(history -> 
            history.removeIf(event -> event.timestamp().isBefore(cutoff))
        );
    }

    private void analyzeCustomerPatterns() {
        // Analyze transaction patterns for all customers
    }

    private void generateComplianceReports() {
        // Generate regulatory compliance reports
    }

    private void updateRiskProfiles() {
        // Update risk profiles for all customers
    }

    private void notifyComplianceTeam(ComplianceViolation violation) {
        System.out.println("COMPLIANCE ALERT: " + violation);
    }

    private void flagForReview(ComplianceViolation violation) {
        System.out.println("FLAGGED FOR REVIEW: " + violation.violationId());
    }

    private void enhanceMonitoring(String customerId) {
        System.out.println("ENHANCED MONITORING: " + customerId);
    }

    private void logViolation(ComplianceViolation violation) {
        System.out.println("VIOLATION LOGGED: " + violation.violationId());
    }

    // Result and report classes
    public record ComplianceMonitoringResult(
        String transactionId,
        boolean approved,
        List<ComplianceViolation> violations,
        List<String> alerts,
        List<String> recommendations
    ) {}

    public record CTRReport(
        String reportId,
        String customerId,
        String transactionId,
        Money amount,
        TransactionType type,
        Instant generatedAt,
        String generatedBy,
        Map<String, String> metadata
    ) {}

    public record SARReport(
        String reportId,
        String customerId,
        String transactionId,
        String description,
        SuspiciousActivityType activityType,
        Instant generatedAt,
        String generatedBy,
        Map<String, String> metadata
    ) {}

    public enum SuspiciousActivityType {
        UNUSUAL_TRANSACTION_PATTERN, STRUCTURING, VELOCITY_ANOMALY, 
        GEOGRAPHIC_ANOMALY, AMOUNT_ANOMALY, TIMING_ANOMALY
    }

    public record ComplianceMonitoringDashboard(
        long totalTransactions,
        long totalViolations,
        long amlAlerts,
        long largeTransactionReports,
        long blockedTransactions,
        int blockedCustomers,
        int activeViolations,
        long highRiskCustomers,
        Map<ViolationType, Long> violationsByType,
        Map<ViolationSeverity, Long> violationsBySeverity
    ) {}
}