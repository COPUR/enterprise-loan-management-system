package com.bank.infrastructure.audit;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compliance Rule Engine for Banking Regulations
 * 
 * Implements regulatory compliance rules for:
 * - SOX (Sarbanes-Oxley Act)
 * - PCI-DSS (Payment Card Industry Data Security Standard)
 * - GDPR (General Data Protection Regulation)
 * - Basel III (Banking Supervision)
 * - AML/KYC (Anti-Money Laundering / Know Your Customer)
 * - FAPI 2.0 (Financial-grade API)
 */
@Component
public class ComplianceRuleEngine {
    
    private final Map<String, ComplianceRule> rules = new HashMap<>();
    
    public ComplianceRuleEngine() {
        initializeComplianceRules();
    }
    
    /**
     * Initialize compliance rules for various regulations
     */
    private void initializeComplianceRules() {
        // SOX Rules
        rules.put("SOX_FINANCIAL_REPORTING", new ComplianceRule(
            "SOX",
            "Financial Reporting Accuracy",
            event -> event.getCategory() == AuditEvent.EventCategory.TRANSACTION ||
                    event.getCategory() == AuditEvent.EventCategory.DATA_MODIFICATION,
            event -> {
                Map<String, Object> metadata = new HashMap<>(event.getMetadata());
                metadata.put("soxRelevant", true);
                metadata.put("requiresAttestation", true);
                return metadata;
            }
        ));
        
        // PCI-DSS Rules
        rules.put("PCI_CARD_DATA_ACCESS", new ComplianceRule(
            "PCI-DSS",
            "Card Data Access Control",
            event -> event.getCategory() == AuditEvent.EventCategory.DATA_ACCESS &&
                    event.getMetadata() != null && 
                    event.getMetadata().containsKey("cardData"),
            event -> {
                Map<String, Object> metadata = new HashMap<>(event.getMetadata());
                metadata.put("pciScope", true);
                metadata.put("dataClassification", "HIGHLY_SENSITIVE");
                metadata.put("retentionDays", 365);
                return metadata;
            }
        ));
        
        // GDPR Rules
        rules.put("GDPR_PERSONAL_DATA", new ComplianceRule(
            "GDPR",
            "Personal Data Processing",
            event -> event.getCategory() == AuditEvent.EventCategory.DATA_ACCESS ||
                    event.getCategory() == AuditEvent.EventCategory.DATA_MODIFICATION,
            event -> {
                Map<String, Object> metadata = new HashMap<>(event.getMetadata());
                metadata.put("gdprRelevant", true);
                metadata.put("lawfulBasis", determineLawfulBasis(event));
                metadata.put("dataSubjectRights", true);
                return metadata;
            }
        ));
        
        // Basel III Rules
        rules.put("BASEL_RISK_MANAGEMENT", new ComplianceRule(
            "Basel III",
            "Risk Management Controls",
            event -> event.getCategory() == AuditEvent.EventCategory.TRANSACTION &&
                    isHighValueTransaction(event),
            event -> {
                Map<String, Object> metadata = new HashMap<>(event.getMetadata());
                metadata.put("baselIII", true);
                metadata.put("riskCategory", determineRiskCategory(event));
                metadata.put("capitalRequirement", calculateCapitalRequirement(event));
                return metadata;
            }
        ));
        
        // AML/KYC Rules
        rules.put("AML_SUSPICIOUS_ACTIVITY", new ComplianceRule(
            "AML",
            "Suspicious Activity Monitoring",
            event -> event.getCategory() == AuditEvent.EventCategory.FRAUD_DETECTION ||
                    isSuspiciousTransaction(event),
            event -> {
                Map<String, Object> metadata = new HashMap<>(event.getMetadata());
                metadata.put("amlRelevant", true);
                metadata.put("sarRequired", shouldFileSAR(event));
                metadata.put("kycStatus", "VERIFIED");
                return metadata;
            }
        ));
        
        // FAPI 2.0 Rules
        rules.put("FAPI_SECURITY_PROFILE", new ComplianceRule(
            "FAPI2",
            "Financial-grade API Security",
            event -> event.getCategory() == AuditEvent.EventCategory.AUTHENTICATION ||
                    event.getCategory() == AuditEvent.EventCategory.AUTHORIZATION,
            event -> {
                Map<String, Object> metadata = new HashMap<>(event.getMetadata());
                metadata.put("fapiCompliant", true);
                metadata.put("mtlsRequired", true);
                metadata.put("parRequired", true);
                return metadata;
            }
        ));
    }
    
    /**
     * Enrich audit event with compliance metadata
     */
    public AuditEvent enrichEvent(AuditEvent event) {
        Map<String, Object> enrichedMetadata = event.getMetadata() != null ? 
            new HashMap<>(event.getMetadata()) : new HashMap<>();
        
        // Apply all applicable compliance rules
        for (ComplianceRule rule : rules.values()) {
            if (rule.isApplicable(event)) {
                Map<String, Object> ruleMetadata = rule.enrich(event);
                enrichedMetadata.putAll(ruleMetadata);
            }
        }
        
        // Add standard compliance metadata
        enrichedMetadata.put("complianceChecked", true);
        enrichedMetadata.put("complianceTimestamp", LocalDateTime.now());
        enrichedMetadata.put("retentionPolicy", determineRetentionPolicy(event));
        
        // Create new event with enriched metadata
        return AuditEvent.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .category(event.getCategory())
            .severity(event.getSeverity())
            .userId(event.getUserId())
            .customerId(event.getCustomerId())
            .sessionId(event.getSessionId())
            .ipAddress(event.getIpAddress())
            .userAgent(event.getUserAgent())
            .resource(event.getResource())
            .action(event.getAction())
            .result(event.getResult())
            .description(event.getDescription())
            .metadata(enrichedMetadata)
            .timestamp(event.getTimestamp())
            .correlationId(event.getCorrelationId())
            .applicationName(event.getApplicationName())
            .applicationVersion(event.getApplicationVersion())
            .build();
    }
    
    /**
     * Generate compliance report for specific regulation
     */
    public ComplianceReport generateReport(String regulation, List<AuditEvent> events) {
        ComplianceReport report = new ComplianceReport(regulation);
        
        // Filter events by regulation
        List<AuditEvent> relevantEvents = events.stream()
            .filter(event -> isRelevantForRegulation(event, regulation))
            .collect(Collectors.toList());
        
        // Analyze compliance status
        for (AuditEvent event : relevantEvents) {
            analyzeCompliance(event, report);
        }
        
        // Calculate compliance metrics
        report.calculateMetrics();
        
        return report;
    }
    
    /**
     * Check if event is relevant for specific regulation
     */
    private boolean isRelevantForRegulation(AuditEvent event, String regulation) {
        Map<String, Object> metadata = event.getMetadata();
        if (metadata == null) return false;
        
        switch (regulation) {
            case "SOX":
                return metadata.containsKey("soxRelevant");
            case "PCI-DSS":
                return metadata.containsKey("pciScope");
            case "GDPR":
                return metadata.containsKey("gdprRelevant");
            case "Basel III":
                return metadata.containsKey("baselIII");
            case "AML":
                return metadata.containsKey("amlRelevant");
            case "FAPI2":
                return metadata.containsKey("fapiCompliant");
            default:
                return false;
        }
    }
    
    /**
     * Analyze compliance for specific event
     */
    private void analyzeCompliance(AuditEvent event, ComplianceReport report) {
        // Check for compliance violations
        List<ComplianceViolation> violations = checkViolations(event);
        report.addViolations(violations);
        
        // Track compliance metrics
        report.incrementEventCount();
        if (violations.isEmpty()) {
            report.incrementCompliantCount();
        }
        
        // Add event to report
        report.addEvent(event);
    }
    
    /**
     * Check for compliance violations in event
     */
    private List<ComplianceViolation> checkViolations(AuditEvent event) {
        List<ComplianceViolation> violations = new ArrayList<>();
        
        // Check authentication violations
        if (event.getCategory() == AuditEvent.EventCategory.AUTHENTICATION &&
            event.getResult() == AuditEvent.ActionResult.FAILURE) {
            if (countRecentFailures(event) > 5) {
                violations.add(new ComplianceViolation(
                    "AUTH_BRUTE_FORCE",
                    "Excessive authentication failures detected",
                    ComplianceViolation.Severity.HIGH
                ));
            }
        }
        
        // Check data access violations
        if (event.getCategory() == AuditEvent.EventCategory.DATA_ACCESS) {
            if (!hasValidPurpose(event)) {
                violations.add(new ComplianceViolation(
                    "GDPR_NO_PURPOSE",
                    "Data access without valid purpose",
                    ComplianceViolation.Severity.CRITICAL
                ));
            }
        }
        
        // Check transaction violations
        if (event.getCategory() == AuditEvent.EventCategory.TRANSACTION) {
            if (isHighRiskTransaction(event) && !hasProperApproval(event)) {
                violations.add(new ComplianceViolation(
                    "SOX_NO_APPROVAL",
                    "High-risk transaction without proper approval",
                    ComplianceViolation.Severity.HIGH
                ));
            }
        }
        
        return violations;
    }
    
    // Helper methods
    private String determineLawfulBasis(AuditEvent event) {
        // Simplified logic - in production would be more complex
        if (event.getMetadata() != null && event.getMetadata().containsKey("consent")) {
            return "CONSENT";
        } else if (event.getAction() != null && event.getAction().contains("CONTRACT")) {
            return "CONTRACT";
        } else {
            return "LEGITIMATE_INTEREST";
        }
    }
    
    private boolean isHighValueTransaction(AuditEvent event) {
        if (event.getMetadata() != null && event.getMetadata().containsKey("amount")) {
            Object amount = event.getMetadata().get("amount");
            if (amount instanceof Number) {
                return ((Number) amount).doubleValue() > 100000;
            }
        }
        return false;
    }
    
    private String determineRiskCategory(AuditEvent event) {
        if (isHighValueTransaction(event)) {
            return "HIGH";
        } else if (event.getMetadata() != null && event.getMetadata().containsKey("riskScore")) {
            Object riskScore = event.getMetadata().get("riskScore");
            if (riskScore instanceof Number) {
                int score = ((Number) riskScore).intValue();
                if (score > 75) return "HIGH";
                if (score > 40) return "MEDIUM";
            }
        }
        return "LOW";
    }
    
    private double calculateCapitalRequirement(AuditEvent event) {
        // Simplified Basel III capital calculation
        if (event.getMetadata() != null && event.getMetadata().containsKey("amount")) {
            Object amount = event.getMetadata().get("amount");
            if (amount instanceof Number) {
                double value = ((Number) amount).doubleValue();
                String riskCategory = determineRiskCategory(event);
                switch (riskCategory) {
                    case "HIGH": return value * 0.15;
                    case "MEDIUM": return value * 0.08;
                    default: return value * 0.04;
                }
            }
        }
        return 0.0;
    }
    
    private boolean isSuspiciousTransaction(AuditEvent event) {
        if (event.getCategory() != AuditEvent.EventCategory.TRANSACTION) {
            return false;
        }
        
        // Check for suspicious patterns
        if (event.getMetadata() != null) {
            // High risk score
            Object riskScore = event.getMetadata().get("riskScore");
            if (riskScore instanceof Number && ((Number) riskScore).intValue() > 75) {
                return true;
            }
            
            // Unusual patterns
            Object pattern = event.getMetadata().get("pattern");
            if (pattern != null && pattern.toString().contains("UNUSUAL")) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean shouldFileSAR(AuditEvent event) {
        // Suspicious Activity Report criteria
        if (event.getMetadata() != null) {
            Object riskScore = event.getMetadata().get("riskScore");
            if (riskScore instanceof Number && ((Number) riskScore).intValue() > 90) {
                return true;
            }
            
            Object blocked = event.getMetadata().get("blocked");
            if (blocked instanceof Boolean && (Boolean) blocked) {
                return true;
            }
        }
        
        return false;
    }
    
    private String determineRetentionPolicy(AuditEvent event) {
        // Determine retention period based on regulation
        if (event.getMetadata() != null) {
            if (event.getMetadata().containsKey("pciScope")) {
                return "1_YEAR";
            } else if (event.getMetadata().containsKey("soxRelevant")) {
                return "7_YEARS";
            } else if (event.getMetadata().containsKey("amlRelevant")) {
                return "5_YEARS";
            }
        }
        
        // Default retention
        return "3_YEARS";
    }
    
    private int countRecentFailures(AuditEvent event) {
        // In production, this would query the audit repository
        return 3; // Mock value
    }
    
    private boolean hasValidPurpose(AuditEvent event) {
        return event.getMetadata() != null && event.getMetadata().containsKey("purpose");
    }
    
    private boolean isHighRiskTransaction(AuditEvent event) {
        return "HIGH".equals(determineRiskCategory(event));
    }
    
    private boolean hasProperApproval(AuditEvent event) {
        return event.getMetadata() != null && event.getMetadata().containsKey("approvedBy");
    }
    
    /**
     * Compliance rule definition
     */
    private static class ComplianceRule {
        private final String regulation;
        private final String description;
        private final java.util.function.Predicate<AuditEvent> applicabilityCheck;
        private final java.util.function.Function<AuditEvent, Map<String, Object>> enrichmentFunction;
        
        public ComplianceRule(String regulation, String description,
                            java.util.function.Predicate<AuditEvent> applicabilityCheck,
                            java.util.function.Function<AuditEvent, Map<String, Object>> enrichmentFunction) {
            this.regulation = regulation;
            this.description = description;
            this.applicabilityCheck = applicabilityCheck;
            this.enrichmentFunction = enrichmentFunction;
        }
        
        public boolean isApplicable(AuditEvent event) {
            return applicabilityCheck.test(event);
        }
        
        public Map<String, Object> enrich(AuditEvent event) {
            return enrichmentFunction.apply(event);
        }
    }
}