package com.bank.loan.loan.service;

import com.bank.loan.loan.entity.Loan;
import com.bank.loan.loan.entity.Payment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive Audit Service for Banking Operations
 * 
 * Provides banking-grade audit logging for all security-sensitive operations:
 * - FAPI 2.0 + DPoP compliance logging
 * - Financial transaction audit trails
 * - Security violation tracking
 * - Regulatory compliance reporting
 * - SOX and PCI DSS audit requirements
 */
@Service
public class AuditService {

    @Autowired
    private ObjectMapper objectMapper;

    // In production, this would use a proper audit logging framework
    // like ELK stack, Splunk, or dedicated audit database
    private static final org.slf4j.Logger auditLogger = 
        org.slf4j.LoggerFactory.getLogger("BANKING_AUDIT");

    /**
     * Log loan creation with comprehensive audit trail
     */
    public void logLoanCreation(Loan loan, String userId, String ipAddress, 
                               String fiapiInteractionId, String userAgent) {
        try {
            Map<String, Object> auditEntry = createBaseAuditEntry(
                "LOAN_CREATED", userId, ipAddress, fiapiInteractionId, userAgent);
            
            auditEntry.put("loan_id", loan.getLoanId());
            auditEntry.put("customer_id", loan.getCustomerId());
            auditEntry.put("loan_amount", loan.getAmount());
            auditEntry.put("loan_type", loan.getLoanType());
            auditEntry.put("loan_status", loan.getStatus());
            auditEntry.put("interest_rate", loan.getInterestRate());
            auditEntry.put("installment_count", loan.getInstallmentCount());
            
            // Sensitive data handling - only log business identifiers
            auditEntry.put("compliance_category", "LOAN_ORIGINATION");
            auditEntry.put("regulatory_impact", "TILA_DISCLOSURE_REQUIRED");
            
            logAuditEntry(auditEntry);
            
        } catch (Exception e) {
            // Audit logging failures should not break business operations
            auditLogger.error("Failed to log loan creation audit entry", e);
        }
    }

    /**
     * Log loan approval with regulatory compliance tracking
     */
    public void logLoanApproval(Loan loan, String userId, String ipAddress, 
                               String fiapiInteractionId, String userAgent) {
        try {
            Map<String, Object> auditEntry = createBaseAuditEntry(
                "LOAN_APPROVED", userId, ipAddress, fiapiInteractionId, userAgent);
            
            auditEntry.put("loan_id", loan.getLoanId());
            auditEntry.put("customer_id", loan.getCustomerId());
            auditEntry.put("approved_amount", loan.getAmount());
            auditEntry.put("approved_by", loan.getApprovedBy());
            auditEntry.put("approval_date", loan.getApprovedDate());
            auditEntry.put("approval_conditions", loan.getApprovalConditions());
            
            auditEntry.put("compliance_category", "LOAN_APPROVAL");
            auditEntry.put("regulatory_impact", "FAIR_LENDING_DOCUMENTATION");
            auditEntry.put("risk_level", determineLoanRiskLevel(loan));
            
            logAuditEntry(auditEntry);
            
        } catch (Exception e) {
            auditLogger.error("Failed to log loan approval audit entry", e);
        }
    }

    /**
     * Log payment processing with banking compliance tracking
     */
    public void logPaymentProcessing(Payment payment, String userId, String ipAddress, 
                                   String fiapiInteractionId, String userAgent) {
        try {
            Map<String, Object> auditEntry = createBaseAuditEntry(
                "PAYMENT_PROCESSED", userId, ipAddress, fiapiInteractionId, userAgent);
            
            auditEntry.put("payment_id", payment.getPaymentId());
            auditEntry.put("loan_id", payment.getLoanId());
            auditEntry.put("payment_amount", payment.getAmount());
            auditEntry.put("payment_type", payment.getPaymentType());
            auditEntry.put("payment_method", payment.getPaymentMethodType());
            auditEntry.put("payment_reference", payment.getPaymentReference());
            auditEntry.put("principal_amount", payment.getPrincipalAmount());
            auditEntry.put("interest_amount", payment.getInterestAmount());
            auditEntry.put("late_fee", payment.getLateFee());
            
            auditEntry.put("compliance_category", "PAYMENT_PROCESSING");
            auditEntry.put("regulatory_impact", "FDCPA_PAYMENT_ALLOCATION");
            auditEntry.put("payment_allocation_applied", true);
            
            logAuditEntry(auditEntry);
            
        } catch (Exception e) {
            auditLogger.error("Failed to log payment processing audit entry", e);
        }
    }

    /**
     * Log security violations for FAPI 2.0 + DPoP compliance
     */
    public void logSecurityViolation(String violationType, String message, String userId, 
                                   String ipAddress, String fiapiInteractionId) {
        try {
            Map<String, Object> auditEntry = createBaseAuditEntry(
                "SECURITY_VIOLATION", userId, ipAddress, fiapiInteractionId, "SECURITY_SYSTEM");
            
            auditEntry.put("violation_type", violationType);
            auditEntry.put("violation_message", message);
            auditEntry.put("security_severity", determineSecuritySeverity(violationType));
            auditEntry.put("requires_investigation", shouldRequireInvestigation(violationType));
            
            auditEntry.put("compliance_category", "SECURITY_VIOLATION");
            auditEntry.put("regulatory_impact", "FAPI_SECURITY_COMPLIANCE");
            auditEntry.put("dpop_compliance", true);
            
            // Log at ERROR level for security violations
            logSecurityViolationEntry(auditEntry);
            
        } catch (Exception e) {
            auditLogger.error("Failed to log security violation audit entry", e);
        }
    }

    /**
     * Log data access for privacy compliance (GDPR, CCPA)
     */
    public void logDataAccess(String operation, String entityId, String userId, 
                             String ipAddress, String fiapiInteractionId) {
        try {
            Map<String, Object> auditEntry = createBaseAuditEntry(
                "DATA_ACCESS", userId, ipAddress, fiapiInteractionId, "APPLICATION");
            
            auditEntry.put("access_operation", operation);
            auditEntry.put("entity_id", entityId);
            auditEntry.put("data_classification", determineDataClassification(operation));
            auditEntry.put("access_justification", "BUSINESS_OPERATION");
            
            auditEntry.put("compliance_category", "DATA_ACCESS");
            auditEntry.put("regulatory_impact", "PRIVACY_COMPLIANCE");
            auditEntry.put("gdpr_applicable", true);
            
            logAuditEntry(auditEntry);
            
        } catch (Exception e) {
            auditLogger.error("Failed to log data access audit entry", e);
        }
    }

    /**
     * Log FAPI 2.0 authentication events
     */
    public void logFAPIAuthentication(String clientId, String userId, String dpopJkt, 
                                    String operation, boolean success, String ipAddress, 
                                    String fiapiInteractionId) {
        try {
            Map<String, Object> auditEntry = createBaseAuditEntry(
                success ? "FAPI_AUTH_SUCCESS" : "FAPI_AUTH_FAILURE", 
                userId, ipAddress, fiapiInteractionId, "FAPI_SECURITY_FILTER");
            
            auditEntry.put("client_id", clientId);
            auditEntry.put("dpop_jkt_thumbprint", dpopJkt);
            auditEntry.put("auth_operation", operation);
            auditEntry.put("auth_success", success);
            
            auditEntry.put("compliance_category", "AUTHENTICATION");
            auditEntry.put("regulatory_impact", "FAPI2_COMPLIANCE");
            auditEntry.put("security_profile", "FAPI2_DPOP");
            
            if (success) {
                logAuditEntry(auditEntry);
            } else {
                logSecurityViolationEntry(auditEntry);
            }
            
        } catch (Exception e) {
            auditLogger.error("Failed to log FAPI authentication audit entry", e);
        }
    }

    /**
     * Log regulatory compliance events
     */
    public void logComplianceEvent(String complianceType, String entityId, String details, 
                                 String userId, String ipAddress, String fiapiInteractionId) {
        try {
            Map<String, Object> auditEntry = createBaseAuditEntry(
                "COMPLIANCE_EVENT", userId, ipAddress, fiapiInteractionId, "COMPLIANCE_SYSTEM");
            
            auditEntry.put("compliance_type", complianceType);
            auditEntry.put("entity_id", entityId);
            auditEntry.put("compliance_details", details);
            auditEntry.put("compliance_status", "DOCUMENTED");
            
            auditEntry.put("compliance_category", complianceType);
            auditEntry.put("regulatory_impact", "COMPLIANCE_DOCUMENTATION");
            
            logAuditEntry(auditEntry);
            
        } catch (Exception e) {
            auditLogger.error("Failed to log compliance event audit entry", e);
        }
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    private Map<String, Object> createBaseAuditEntry(String eventType, String userId, 
                                                    String ipAddress, String fiapiInteractionId, 
                                                    String userAgent) {
        Map<String, Object> auditEntry = new HashMap<>();
        
        auditEntry.put("audit_id", UUID.randomUUID().toString());
        auditEntry.put("timestamp", LocalDateTime.now().toString());
        auditEntry.put("event_type", eventType);
        auditEntry.put("user_id", userId);
        auditEntry.put("ip_address", ipAddress);
        auditEntry.put("fapi_interaction_id", fiapiInteractionId);
        auditEntry.put("user_agent", userAgent);
        auditEntry.put("application", "enterprise-loan-management");
        auditEntry.put("version", "1.0.0");
        auditEntry.put("environment", getEnvironment());
        
        return auditEntry;
    }

    private void logAuditEntry(Map<String, Object> auditEntry) {
        try {
            String auditJson = objectMapper.writeValueAsString(auditEntry);
            auditLogger.info("AUDIT: {}", auditJson);
        } catch (Exception e) {
            auditLogger.error("Failed to serialize audit entry", e);
        }
    }

    private void logSecurityViolationEntry(Map<String, Object> auditEntry) {
        try {
            String auditJson = objectMapper.writeValueAsString(auditEntry);
            auditLogger.error("SECURITY_VIOLATION: {}", auditJson);
        } catch (Exception e) {
            auditLogger.error("Failed to serialize security violation audit entry", e);
        }
    }

    private String determineLoanRiskLevel(Loan loan) {
        // In production, this would use proper risk assessment
        if (loan.getAmount().doubleValue() > 500000) {
            return "HIGH";
        } else if (loan.getAmount().doubleValue() > 100000) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String determineSecuritySeverity(String violationType) {
        return switch (violationType) {
            case "LOAN_CREATION_FAILED", "PAYMENT_PROCESSING_FAILED" -> "MEDIUM";
            case "LOAN_ACCESS_DENIED", "PAYMENT_ACCESS_DENIED" -> "HIGH";
            case "INSTALLMENT_ACCESS_DENIED", "PAYMENT_HISTORY_ACCESS_DENIED" -> "MEDIUM";
            case "FAPI_VIOLATION", "DPOP_VIOLATION" -> "CRITICAL";
            default -> "LOW";
        };
    }

    private boolean shouldRequireInvestigation(String violationType) {
        return violationType.contains("FAPI") || 
               violationType.contains("DPOP") || 
               violationType.contains("ACCESS_DENIED");
    }

    private String determineDataClassification(String operation) {
        return switch (operation) {
            case "LOANS_VIEWED", "LOAN_DETAILS_VIEWED" -> "CONFIDENTIAL";
            case "PAYMENTS_VIEWED", "PAYMENT_HISTORY_VIEWED" -> "CONFIDENTIAL";
            case "INSTALLMENTS_VIEWED" -> "INTERNAL";
            case "CUSTOMER_PROFILE_VIEWED" -> "CONFIDENTIAL";
            default -> "INTERNAL";
        };
    }

    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "development");
    }
}