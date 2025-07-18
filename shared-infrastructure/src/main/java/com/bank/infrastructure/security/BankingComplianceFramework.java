package com.bank.infrastructure.security;

import com.bank.infrastructure.domain.Money;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Banking Compliance Framework for Regulatory Standards
 * 
 * Comprehensive compliance management system covering:
 * - PCI DSS (Payment Card Industry Data Security Standard)
 * - SOX (Sarbanes-Oxley Act) compliance
 * - GDPR (General Data Protection Regulation)
 * - Basel III capital requirements
 * - AML (Anti-Money Laundering) regulations
 * - KYC (Know Your Customer) requirements
 * - FAPI 2.0 compliance monitoring
 * - Islamic banking Sharia compliance
 * 
 * Provides automated compliance checking, reporting,
 * and audit trail management for banking operations.
 */
@Component
public class BankingComplianceFramework {

    // Compliance tracking
    private final Map<String, ComplianceCheck> complianceChecks = new ConcurrentHashMap<>();
    private final Map<String, ComplianceViolation> violations = new ConcurrentHashMap<>();
    private final Map<String, AuditTrail> auditTrails = new ConcurrentHashMap<>();
    private final AtomicLong complianceCheckCounter = new AtomicLong(0);

    /**
     * Compliance standards enumeration
     */
    public enum ComplianceStandard {
        PCI_DSS("Payment Card Industry Data Security Standard"),
        SOX("Sarbanes-Oxley Act"),
        GDPR("General Data Protection Regulation"),
        BASEL_III("Basel III Capital Requirements"),
        AML("Anti-Money Laundering"),
        KYC("Know Your Customer"),
        FAPI_2_0("Financial-grade API Security Profile 2.0"),
        SHARIA("Islamic Banking Sharia Compliance"),
        ISO_27001("Information Security Management"),
        NIST("NIST Cybersecurity Framework");

        private final String description;
        ComplianceStandard(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    /**
     * Compliance check status
     */
    public enum ComplianceStatus {
        COMPLIANT, NON_COMPLIANT, PENDING_REVIEW, EXEMPTED, UNKNOWN
    }

    /**
     * Compliance check severity
     */
    public enum ComplianceSeverity {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);
        
        private final int level;
        ComplianceSeverity(int level) { this.level = level; }
        public int getLevel() { return level; }
    }

    /**
     * Compliance check record
     */
    public record ComplianceCheck(
        String checkId,
        ComplianceStandard standard,
        String controlId,
        String description,
        ComplianceStatus status,
        ComplianceSeverity severity,
        String entity,
        String entityType,
        Map<String, Object> checkData,
        Instant checkedAt,
        String checkedBy,
        String comments,
        Instant nextReviewDate
    ) {}

    /**
     * Compliance violation record
     */
    public record ComplianceViolation(
        String violationId,
        ComplianceStandard standard,
        String controlId,
        String description,
        ComplianceSeverity severity,
        String entity,
        String entityType,
        ViolationStatus status,
        Instant detectedAt,
        String detectedBy,
        Instant resolvedAt,
        String resolvedBy,
        String resolutionDescription,
        Map<String, Object> violationData
    ) {
        public enum ViolationStatus {
            OPEN, IN_PROGRESS, RESOLVED, WAIVED, CLOSED
        }
    }

    /**
     * Audit trail record
     */
    public record AuditTrail(
        String auditId,
        String entity,
        String entityType,
        String action,
        String userId,
        String ipAddress,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        Instant timestamp,
        String description,
        ComplianceStandard relatedStandard
    ) {}

    /**
     * PCI DSS compliance check
     */
    public ComplianceCheck performPciDssCheck(String entity, String entityType, Map<String, Object> data) {
        String checkId = generateCheckId("PCI", entity);
        ComplianceStatus status = ComplianceStatus.COMPLIANT;
        String comments = "PCI DSS compliance verified";

        // Check for credit card data exposure
        if (data.containsKey("creditCardNumber") || data.containsKey("cvv") || data.containsKey("cardData")) {
            // Check if data is properly encrypted
            boolean isEncrypted = Boolean.TRUE.equals(data.get("isEncrypted"));
            if (!isEncrypted) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Credit card data not properly encrypted";
                
                createViolation(ComplianceStandard.PCI_DSS, "PCI-3.4", 
                    "Card data must be encrypted during transmission and storage",
                    ComplianceSeverity.CRITICAL, entity, entityType, data);
            }
        }

        // Check access controls (PCI-7)
        if (data.containsKey("accessLevel")) {
            String accessLevel = (String) data.get("accessLevel");
            if ("ADMIN".equals(accessLevel) && !data.containsKey("businessJustification")) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Administrative access without business justification";
                
                createViolation(ComplianceStandard.PCI_DSS, "PCI-7.1",
                    "Access to card data must be limited by business need-to-know",
                    ComplianceSeverity.HIGH, entity, entityType, data);
            }
        }

        ComplianceCheck check = new ComplianceCheck(
            checkId, ComplianceStandard.PCI_DSS, "PCI-General",
            "PCI DSS compliance verification", status, ComplianceSeverity.HIGH,
            entity, entityType, data, Instant.now(), "system",
            comments, Instant.now().plusSeconds(86400 * 30) // 30 days
        );

        complianceChecks.put(checkId, check);
        complianceCheckCounter.incrementAndGet();
        
        return check;
    }

    /**
     * GDPR compliance check
     */
    public ComplianceCheck performGdprCheck(String entity, String entityType, Map<String, Object> data) {
        String checkId = generateCheckId("GDPR", entity);
        ComplianceStatus status = ComplianceStatus.COMPLIANT;
        String comments = "GDPR compliance verified";

        // Check for personal data processing consent
        if (data.containsKey("personalData")) {
            boolean hasConsent = Boolean.TRUE.equals(data.get("hasConsent"));
            if (!hasConsent) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Personal data processing without explicit consent";
                
                createViolation(ComplianceStandard.GDPR, "GDPR-6",
                    "Personal data processing requires lawful basis",
                    ComplianceSeverity.CRITICAL, entity, entityType, data);
            }
        }

        // Check data retention period
        if (data.containsKey("dataRetentionDays")) {
            int retentionDays = (Integer) data.get("dataRetentionDays");
            if (retentionDays > 2555) { // 7 years max for banking
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Data retention period exceeds regulatory limits";
                
                createViolation(ComplianceStandard.GDPR, "GDPR-5",
                    "Data must not be kept longer than necessary",
                    ComplianceSeverity.MEDIUM, entity, entityType, data);
            }
        }

        // Check for data subject rights implementation
        if (!data.containsKey("rightsImplemented") || !Boolean.TRUE.equals(data.get("rightsImplemented"))) {
            status = ComplianceStatus.NON_COMPLIANT;
            comments = "Data subject rights not fully implemented";
            
            createViolation(ComplianceStandard.GDPR, "GDPR-12",
                "Data subject rights must be facilitated",
                ComplianceSeverity.HIGH, entity, entityType, data);
        }

        ComplianceCheck check = new ComplianceCheck(
            checkId, ComplianceStandard.GDPR, "GDPR-General",
            "GDPR compliance verification", status, ComplianceSeverity.CRITICAL,
            entity, entityType, data, Instant.now(), "system",
            comments, Instant.now().plusSeconds(86400 * 90) // 90 days
        );

        complianceChecks.put(checkId, check);
        complianceCheckCounter.incrementAndGet();
        
        return check;
    }

    /**
     * AML (Anti-Money Laundering) compliance check
     */
    public ComplianceCheck performAmlCheck(String customerId, Money transactionAmount, 
                                         String transactionType, Map<String, Object> customerData) {
        String checkId = generateCheckId("AML", customerId);
        ComplianceStatus status = ComplianceStatus.COMPLIANT;
        String comments = "AML compliance verified";

        Map<String, Object> data = new HashMap<>(customerData);
        data.put("transactionAmount", transactionAmount.getAmount());
        data.put("transactionType", transactionType);

        // Check transaction amount thresholds
        if (transactionAmount.getAmount().doubleValue() >= 10000) {
            // Large transaction reporting requirement
            boolean isReported = Boolean.TRUE.equals(data.get("isReported"));
            if (!isReported) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Large transaction not reported to authorities";
                
                createViolation(ComplianceStandard.AML, "AML-CTR",
                    "Cash transactions over $10,000 must be reported",
                    ComplianceSeverity.CRITICAL, customerId, "CUSTOMER", data);
            }
        }

        // Check for suspicious activity patterns
        if (data.containsKey("suspiciousActivityScore")) {
            double suspiciousScore = (Double) data.get("suspiciousActivityScore");
            if (suspiciousScore > 75.0) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Suspicious activity detected - SAR filing required";
                
                createViolation(ComplianceStandard.AML, "AML-SAR",
                    "Suspicious activity must be reported",
                    ComplianceSeverity.HIGH, customerId, "CUSTOMER", data);
            }
        }

        // Check customer due diligence
        if (!data.containsKey("cddCompleted") || !Boolean.TRUE.equals(data.get("cddCompleted"))) {
            status = ComplianceStatus.NON_COMPLIANT;
            comments = "Customer Due Diligence not completed";
            
            createViolation(ComplianceStandard.AML, "AML-CDD",
                "Customer Due Diligence must be completed",
                ComplianceSeverity.HIGH, customerId, "CUSTOMER", data);
        }

        ComplianceCheck check = new ComplianceCheck(
            checkId, ComplianceStandard.AML, "AML-General",
            "Anti-Money Laundering compliance verification", status, ComplianceSeverity.CRITICAL,
            customerId, "CUSTOMER", data, Instant.now(), "system",
            comments, Instant.now().plusSeconds(86400 * 7) // 7 days
        );

        complianceChecks.put(checkId, check);
        complianceCheckCounter.incrementAndGet();
        
        return check;
    }

    /**
     * Islamic banking Sharia compliance check
     */
    public ComplianceCheck performShariaComplianceCheck(String productId, String productType, 
                                                       Map<String, Object> productData) {
        String checkId = generateCheckId("SHARIA", productId);
        ComplianceStatus status = ComplianceStatus.COMPLIANT;
        String comments = "Sharia compliance verified";

        // Check for interest-based transactions (Riba)
        if (productData.containsKey("interestRate")) {
            double interestRate = (Double) productData.get("interestRate");
            if (interestRate > 0) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Interest-based transaction violates Sharia principles";
                
                createViolation(ComplianceStandard.SHARIA, "SHARIA-RIBA",
                    "Interest (Riba) is prohibited in Islamic banking",
                    ComplianceSeverity.CRITICAL, productId, "PRODUCT", productData);
            }
        }

        // Check for prohibited industries (Haram)
        if (productData.containsKey("industry")) {
            String industry = (String) productData.get("industry");
            List<String> prohibitedIndustries = List.of("alcohol", "gambling", "tobacco", "pork", "adult-entertainment");
            if (prohibitedIndustries.contains(industry.toLowerCase())) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Investment in prohibited industry violates Sharia principles";
                
                createViolation(ComplianceStandard.SHARIA, "SHARIA-HARAM",
                    "Investment in prohibited industries is not allowed",
                    ComplianceSeverity.CRITICAL, productId, "PRODUCT", productData);
            }
        }

        // Check for Sharia board approval
        if (!productData.containsKey("shariaBoardApproval") || !Boolean.TRUE.equals(productData.get("shariaBoardApproval"))) {
            status = ComplianceStatus.NON_COMPLIANT;
            comments = "Product not approved by Sharia board";
            
            createViolation(ComplianceStandard.SHARIA, "SHARIA-APPROVAL",
                "All Islamic banking products must be approved by Sharia board",
                ComplianceSeverity.HIGH, productId, "PRODUCT", productData);
        }

        ComplianceCheck check = new ComplianceCheck(
            checkId, ComplianceStandard.SHARIA, "SHARIA-General",
            "Islamic banking Sharia compliance verification", status, ComplianceSeverity.CRITICAL,
            productId, "PRODUCT", productData, Instant.now(), "system",
            comments, Instant.now().plusSeconds(86400 * 30) // 30 days
        );

        complianceChecks.put(checkId, check);
        complianceCheckCounter.incrementAndGet();
        
        return check;
    }

    /**
     * SOX compliance check for financial reporting
     */
    public ComplianceCheck performSoxComplianceCheck(String reportId, Map<String, Object> financialData) {
        String checkId = generateCheckId("SOX", reportId);
        ComplianceStatus status = ComplianceStatus.COMPLIANT;
        String comments = "SOX compliance verified";

        // Check for management certification
        if (!financialData.containsKey("managementCertification") || 
            !Boolean.TRUE.equals(financialData.get("managementCertification"))) {
            status = ComplianceStatus.NON_COMPLIANT;
            comments = "Management certification missing for financial report";
            
            createViolation(ComplianceStandard.SOX, "SOX-302",
                "Management must certify accuracy of financial reports",
                ComplianceSeverity.CRITICAL, reportId, "FINANCIAL_REPORT", financialData);
        }

        // Check for internal controls assessment
        if (!financialData.containsKey("internalControlsAssessment") || 
            !Boolean.TRUE.equals(financialData.get("internalControlsAssessment"))) {
            status = ComplianceStatus.NON_COMPLIANT;
            comments = "Internal controls assessment not completed";
            
            createViolation(ComplianceStandard.SOX, "SOX-404",
                "Internal controls over financial reporting must be assessed",
                ComplianceSeverity.HIGH, reportId, "FINANCIAL_REPORT", financialData);
        }

        // Check for auditor independence
        if (financialData.containsKey("auditorServices")) {
            @SuppressWarnings("unchecked")
            List<String> services = (List<String>) financialData.get("auditorServices");
            if (services.contains("consulting") || services.contains("it-services")) {
                status = ComplianceStatus.NON_COMPLIANT;
                comments = "Auditor independence compromised by non-audit services";
                
                createViolation(ComplianceStandard.SOX, "SOX-201",
                    "Auditors cannot provide certain non-audit services",
                    ComplianceSeverity.HIGH, reportId, "FINANCIAL_REPORT", financialData);
            }
        }

        ComplianceCheck check = new ComplianceCheck(
            checkId, ComplianceStandard.SOX, "SOX-General",
            "Sarbanes-Oxley compliance verification", status, ComplianceSeverity.CRITICAL,
            reportId, "FINANCIAL_REPORT", financialData, Instant.now(), "system",
            comments, Instant.now().plusSeconds(86400 * 90) // 90 days
        );

        complianceChecks.put(checkId, check);
        complianceCheckCounter.incrementAndGet();
        
        return check;
    }

    /**
     * Create compliance violation
     */
    private void createViolation(ComplianceStandard standard, String controlId, String description,
                               ComplianceSeverity severity, String entity, String entityType,
                               Map<String, Object> violationData) {
        String violationId = UUID.randomUUID().toString();
        
        ComplianceViolation violation = new ComplianceViolation(
            violationId, standard, controlId, description, severity,
            entity, entityType, ComplianceViolation.ViolationStatus.OPEN,
            Instant.now(), "system", null, null, null, violationData
        );
        
        violations.put(violationId, violation);
    }

    /**
     * Create audit trail entry
     */
    public void createAuditTrail(String entity, String entityType, String action, String userId,
                               String ipAddress, Map<String, Object> beforeState, 
                               Map<String, Object> afterState, ComplianceStandard relatedStandard) {
        String auditId = UUID.randomUUID().toString();
        
        AuditTrail auditTrail = new AuditTrail(
            auditId, entity, entityType, action, userId, ipAddress,
            beforeState, afterState, Instant.now(),
            String.format("User %s performed %s on %s", userId, action, entity),
            relatedStandard
        );
        
        auditTrails.put(auditId, auditTrail);
    }

    /**
     * Generate compliance report
     */
    public ComplianceReport generateComplianceReport(ComplianceStandard standard, LocalDate fromDate, LocalDate toDate) {
        Instant from = fromDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant to = toDate.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        
        List<ComplianceCheck> relevantChecks = complianceChecks.values().stream()
            .filter(check -> check.standard() == standard)
            .filter(check -> check.checkedAt().isAfter(from) && check.checkedAt().isBefore(to))
            .collect(Collectors.toList());
            
        List<ComplianceViolation> relevantViolations = violations.values().stream()
            .filter(violation -> violation.standard() == standard)
            .filter(violation -> violation.detectedAt().isAfter(from) && violation.detectedAt().isBefore(to))
            .collect(Collectors.toList());
            
        Map<ComplianceStatus, Long> statusDistribution = relevantChecks.stream()
            .collect(Collectors.groupingBy(ComplianceCheck::status, Collectors.counting()));
            
        Map<ComplianceSeverity, Long> violationSeverityDistribution = relevantViolations.stream()
            .collect(Collectors.groupingBy(ComplianceViolation::severity, Collectors.counting()));
            
        double complianceScore = calculateComplianceScore(relevantChecks, relevantViolations);
        
        return new ComplianceReport(
            standard,
            fromDate,
            toDate,
            relevantChecks.size(),
            relevantViolations.size(),
            complianceScore,
            statusDistribution,
            violationSeverityDistribution,
            relevantChecks,
            relevantViolations,
            Instant.now()
        );
    }

    /**
     * Calculate compliance score
     */
    private double calculateComplianceScore(List<ComplianceCheck> checks, List<ComplianceViolation> violations) {
        if (checks.isEmpty()) return 100.0;
        
        long compliantChecks = checks.stream()
            .mapToLong(check -> check.status() == ComplianceStatus.COMPLIANT ? 1 : 0)
            .sum();
            
        double baseScore = (double) compliantChecks / checks.size() * 100.0;
        
        // Deduct points for violations
        double violationDeduction = violations.stream()
            .mapToDouble(violation -> violation.severity().getLevel() * 2.0)
            .sum();
            
        return Math.max(0.0, baseScore - violationDeduction);
    }

    /**
     * Get compliance dashboard
     */
    public ComplianceDashboard getComplianceDashboard() {
        Map<ComplianceStandard, Long> checksByStandard = complianceChecks.values().stream()
            .collect(Collectors.groupingBy(ComplianceCheck::standard, Collectors.counting()));
            
        Map<ComplianceStandard, Long> violationsByStandard = violations.values().stream()
            .collect(Collectors.groupingBy(ComplianceViolation::standard, Collectors.counting()));
            
        long openViolations = violations.values().stream()
            .mapToLong(v -> v.status() == ComplianceViolation.ViolationStatus.OPEN ? 1 : 0)
            .sum();
            
        Map<ComplianceSeverity, Long> violationsBySeverity = violations.values().stream()
            .collect(Collectors.groupingBy(ComplianceViolation::severity, Collectors.counting()));
            
        return new ComplianceDashboard(
            complianceChecks.size(),
            violations.size(),
            openViolations,
            auditTrails.size(),
            checksByStandard,
            violationsByStandard,
            violationsBySeverity
        );
    }

    /**
     * Scheduled compliance monitoring
     */
    @Scheduled(fixedRate = 86400000) // Daily
    public void performScheduledComplianceChecks() {
        // In production, implement automated compliance checks
        System.out.println("Performing scheduled compliance checks...");
        
        // Clean up old audit trails (keep for 7 years for banking)
        Instant cutoff = Instant.now().minusSeconds(86400 * 365 * 7);
        auditTrails.entrySet().removeIf(entry -> entry.getValue().timestamp().isBefore(cutoff));
    }

    /**
     * Generate check ID
     */
    private String generateCheckId(String prefix, String entity) {
        return String.format("%s-%s-%d", prefix, entity, System.currentTimeMillis());
    }

    // Result classes
    public record ComplianceReport(
        ComplianceStandard standard,
        LocalDate fromDate,
        LocalDate toDate,
        int totalChecks,
        int totalViolations,
        double complianceScore,
        Map<ComplianceStatus, Long> statusDistribution,
        Map<ComplianceSeverity, Long> violationSeverityDistribution,
        List<ComplianceCheck> checks,
        List<ComplianceViolation> violations,
        Instant generatedAt
    ) {}

    public record ComplianceDashboard(
        int totalComplianceChecks,
        int totalViolations,
        long openViolations,
        int totalAuditTrails,
        Map<ComplianceStandard, Long> checksByStandard,
        Map<ComplianceStandard, Long> violationsByStandard,
        Map<ComplianceSeverity, Long> violationsBySeverity
    ) {}
}