package com.bank.infrastructure.audit;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Audit Service for Banking Platform
 * 
 * Provides comprehensive audit logging capabilities for:
 * - Regulatory compliance (SOX, PCI-DSS, GDPR, Basel III)
 * - Security monitoring and incident response
 * - Business activity tracking and analytics
 * - Forensic analysis and investigation
 */
@Service
public class AuditService {
    
    private final AuditEventRepository auditEventRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final ComplianceRuleEngine complianceRuleEngine;
    private final AuditAlertService auditAlertService;
    
    @Autowired
    public AuditService(
            AuditEventRepository auditEventRepository,
            AuditEventPublisher auditEventPublisher,
            ComplianceRuleEngine complianceRuleEngine,
            AuditAlertService auditAlertService) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventPublisher = auditEventPublisher;
        this.complianceRuleEngine = complianceRuleEngine;
        this.auditAlertService = auditAlertService;
    }
    
    /**
     * Log audit event synchronously with compliance checks
     */
    @Transactional
    public AuditEvent logAuditEvent(AuditEvent event) {
        // Validate event
        validateAuditEvent(event);
        
        // Apply compliance rules
        AuditEvent enrichedEvent = complianceRuleEngine.enrichEvent(event);
        
        // Persist event
        AuditEvent savedEvent = auditEventRepository.save(enrichedEvent);
        
        // Publish for real-time monitoring
        auditEventPublisher.publish(savedEvent);
        
        // Check for alerts
        checkAndTriggerAlerts(savedEvent);
        
        return savedEvent;
    }
    
    /**
     * Log audit event asynchronously for high-performance scenarios
     */
    @Async
    public CompletableFuture<AuditEvent> logAuditEventAsync(AuditEvent event) {
        return CompletableFuture.supplyAsync(() -> logAuditEvent(event));
    }
    
    /**
     * Log authentication event
     */
    public void logAuthenticationEvent(String userId, String action, boolean success, String ipAddress) {
        AuditEvent event = AuditEvent.builder()
            .eventType("AUTHENTICATION_" + action.toUpperCase())
            .category(AuditEvent.EventCategory.AUTHENTICATION)
            .severity(success ? AuditEvent.EventSeverity.INFO : AuditEvent.EventSeverity.WARNING)
            .userId(userId)
            .action(action)
            .result(success ? AuditEvent.ActionResult.SUCCESS : AuditEvent.ActionResult.FAILURE)
            .ipAddress(ipAddress)
            .description("User authentication: " + action)
            .metadata(Map.of(
                "authMethod", "OAuth2",
                "mfaEnabled", true
            ))
            .build();
            
        logAuditEvent(event);
    }
    
    /**
     * Log transaction event
     */
    public void logTransactionEvent(String customerId, String transactionType, 
                                  String transactionId, Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
            .eventType("TRANSACTION_" + transactionType.toUpperCase())
            .category(AuditEvent.EventCategory.TRANSACTION)
            .severity(AuditEvent.EventSeverity.INFO)
            .customerId(customerId)
            .resource("Transaction/" + transactionId)
            .action(transactionType)
            .result(AuditEvent.ActionResult.SUCCESS)
            .description("Transaction processed: " + transactionType)
            .metadata(details)
            .build();
            
        logAuditEvent(event);
    }
    
    /**
     * Log data access event for GDPR compliance
     */
    public void logDataAccessEvent(String userId, String resource, String action, 
                                 String purpose, boolean sensitive) {
        AuditEvent event = AuditEvent.builder()
            .eventType("DATA_ACCESS")
            .category(AuditEvent.EventCategory.DATA_ACCESS)
            .severity(sensitive ? AuditEvent.EventSeverity.WARNING : AuditEvent.EventSeverity.INFO)
            .userId(userId)
            .resource(resource)
            .action(action)
            .description("Data access: " + action + " on " + resource)
            .metadata(Map.of(
                "purpose", purpose,
                "sensitiveData", sensitive,
                "gdprRelevant", true
            ))
            .build();
            
        logAuditEvent(event);
    }
    
    /**
     * Log security event
     */
    public void logSecurityEvent(String eventType, String severity, String description, 
                               Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
            .eventType("SECURITY_" + eventType.toUpperCase())
            .category(AuditEvent.EventCategory.SECURITY)
            .severity(AuditEvent.EventSeverity.valueOf(severity.toUpperCase()))
            .description(description)
            .metadata(details)
            .build();
            
        logAuditEvent(event);
    }
    
    /**
     * Log fraud detection event
     */
    public void logFraudDetectionEvent(String customerId, String paymentId, 
                                     int riskScore, List<String> riskFactors, boolean blocked) {
        AuditEvent event = AuditEvent.builder()
            .eventType("FRAUD_DETECTION")
            .category(AuditEvent.EventCategory.FRAUD_DETECTION)
            .severity(blocked ? AuditEvent.EventSeverity.CRITICAL : AuditEvent.EventSeverity.WARNING)
            .customerId(customerId)
            .resource("Payment/" + paymentId)
            .action("FRAUD_CHECK")
            .result(blocked ? AuditEvent.ActionResult.BLOCKED : AuditEvent.ActionResult.SUCCESS)
            .description("Fraud detection: Risk score " + riskScore)
            .metadata(Map.of(
                "riskScore", riskScore,
                "riskFactors", riskFactors,
                "blocked", blocked
            ))
            .build();
            
        logAuditEvent(event);
    }
    
    /**
     * Log compliance event
     */
    public void logComplianceEvent(String regulation, String checkType, 
                                 boolean passed, Map<String, Object> details) {
        AuditEvent event = AuditEvent.builder()
            .eventType("COMPLIANCE_CHECK")
            .category(AuditEvent.EventCategory.COMPLIANCE)
            .severity(passed ? AuditEvent.EventSeverity.INFO : AuditEvent.EventSeverity.ERROR)
            .action(checkType)
            .result(passed ? AuditEvent.ActionResult.SUCCESS : AuditEvent.ActionResult.FAILURE)
            .description("Compliance check for " + regulation)
            .metadata(Map.of(
                "regulation", regulation,
                "checkType", checkType,
                "passed", passed,
                "details", details
            ))
            .build();
            
        logAuditEvent(event);
    }
    
    /**
     * Log administrative action
     */
    public void logAdministrativeAction(String adminId, String action, String target, 
                                      Map<String, Object> changes) {
        AuditEvent event = AuditEvent.builder()
            .eventType("ADMIN_ACTION")
            .category(AuditEvent.EventCategory.ADMINISTRATIVE)
            .severity(AuditEvent.EventSeverity.WARNING)
            .userId(adminId)
            .resource(target)
            .action(action)
            .description("Administrative action: " + action)
            .metadata(Map.of(
                "changes", changes,
                "requiresReview", true
            ))
            .build();
            
        logAuditEvent(event);
    }
    
    /**
     * Query audit events with filters
     */
    public List<AuditEvent> queryAuditEvents(AuditQueryCriteria criteria) {
        return auditEventRepository.findByCriteria(criteria);
    }
    
    /**
     * Get audit trail for specific resource
     */
    public List<AuditEvent> getAuditTrail(String resourceType, String resourceId) {
        return auditEventRepository.findByResource(resourceType + "/" + resourceId);
    }
    
    /**
     * Get user activity history
     */
    public List<AuditEvent> getUserActivityHistory(String userId, LocalDateTime from, LocalDateTime to) {
        return auditEventRepository.findByUserIdAndTimestampBetween(userId, from, to);
    }
    
    /**
     * Get compliance report
     */
    public ComplianceReport generateComplianceReport(String regulation, LocalDateTime from, LocalDateTime to) {
        List<AuditEvent> events = auditEventRepository.findByComplianceRegulation(regulation, from, to);
        return complianceRuleEngine.generateReport(regulation, events);
    }
    
    /**
     * Validate audit event
     */
    private void validateAuditEvent(AuditEvent event) {
        if (event.getEventType() == null || event.getEventType().isEmpty()) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (event.getCategory() == null) {
            throw new IllegalArgumentException("Event category is required");
        }
        // Additional validation as needed
    }
    
    /**
     * Check and trigger alerts based on audit event
     */
    private void checkAndTriggerAlerts(AuditEvent event) {
        // Check for critical security events
        if (event.getCategory() == AuditEvent.EventCategory.SECURITY && 
            event.getSeverity() == AuditEvent.EventSeverity.CRITICAL) {
            auditAlertService.triggerSecurityAlert(event);
        }
        
        // Check for fraud events
        if (event.getCategory() == AuditEvent.EventCategory.FRAUD_DETECTION &&
            event.getResult() == AuditEvent.ActionResult.BLOCKED) {
            auditAlertService.triggerFraudAlert(event);
        }
        
        // Check for compliance failures
        if (event.getCategory() == AuditEvent.EventCategory.COMPLIANCE &&
            event.getResult() == AuditEvent.ActionResult.FAILURE) {
            auditAlertService.triggerComplianceAlert(event);
        }
        
        // Check for repeated authentication failures
        if (event.getCategory() == AuditEvent.EventCategory.AUTHENTICATION &&
            event.getResult() == AuditEvent.ActionResult.FAILURE) {
            checkAuthenticationFailures(event);
        }
    }
    
    /**
     * Check for repeated authentication failures
     */
    private void checkAuthenticationFailures(AuditEvent event) {
        String userId = event.getUserId();
        if (userId != null) {
            long failureCount = auditEventRepository.countRecentAuthenticationFailures(
                userId, LocalDateTime.now().minusMinutes(15)
            );
            
            if (failureCount >= 5) {
                auditAlertService.triggerBruteForceAlert(event, failureCount);
            }
        }
    }
}