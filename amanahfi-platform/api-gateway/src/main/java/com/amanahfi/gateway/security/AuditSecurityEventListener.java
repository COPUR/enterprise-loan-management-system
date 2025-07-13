package com.amanahfi.gateway.security;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.listener.AuditApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Audit Security Event Listener for FAPI 2.0 Compliance
 * 
 * Tracks security events for regulatory compliance:
 * - Authentication attempts (success/failure)
 * - Authorization decisions
 * - Rate limiting violations
 * - DPoP token validation failures
 * - High-value transaction attempts
 * 
 * Audit events are logged for:
 * - CBUAE regulatory reporting
 * - Security incident investigation
 * - Compliance monitoring
 * - Fraud detection
 */
@Component
public class AuditSecurityEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditSecurityEventListener.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    @EventListener
    public void handleAuditEvent(AuditApplicationEvent auditEvent) {
        AuditEvent event = auditEvent.getAuditEvent();
        
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("timestamp", event.getTimestamp());
        auditData.put("principal", event.getPrincipal());
        auditData.put("type", event.getType());
        auditData.put("data", event.getData());
        
        // Log to audit system
        auditLogger.info("Security Event: {}", auditData);
        
        // Handle specific event types
        switch (event.getType()) {
            case "AUTHENTICATION_SUCCESS" -> handleAuthenticationSuccess(event);
            case "AUTHENTICATION_FAILURE" -> handleAuthenticationFailure(event);
            case "ACCESS_DENIED" -> handleAccessDenied(event);
            case "RATE_LIMIT_EXCEEDED" -> handleRateLimitExceeded(event);
            case "DPOP_VALIDATION_FAILED" -> handleDPoPValidationFailed(event);
            case "HIGH_VALUE_TRANSACTION" -> handleHighValueTransaction(event);
            default -> handleGenericSecurityEvent(event);
        }
    }

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String principal = event.getAuthentication().getName();
        String clientId = extractClientId(event.getAuthentication());
        
        Map<String, Object> details = Map.of(
            "principal", principal,
            "clientId", clientId != null ? clientId : "unknown",
            "authenticationMethod", event.getAuthentication().getClass().getSimpleName(),
            "timestamp", Instant.now()
        );
        
        auditLogger.info("Authentication Success: {}", details);
        
        // Track for compliance reporting
        recordComplianceEvent("AUTHENTICATION_SUCCESS", principal, details);
    }

    @EventListener  
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String principal = event.getAuthentication().getName();
        String failureReason = event.getException().getMessage();
        
        Map<String, Object> details = Map.of(
            "principal", principal != null ? principal : "unknown",
            "failureReason", failureReason,
            "exceptionType", event.getException().getClass().getSimpleName(),
            "timestamp", Instant.now()
        );
        
        auditLogger.warn("Authentication Failure: {}", details);
        
        // Alert on suspicious patterns
        checkForSuspiciousActivity(principal, "AUTHENTICATION_FAILURE");
        
        recordComplianceEvent("AUTHENTICATION_FAILURE", principal, details);
    }

    @EventListener
    public void handleAuthorizationDenied(AuthorizationDeniedEvent event) {
        String principal = event.getAuthentication() != null ? 
            event.getAuthentication().get().getName() : "anonymous";
        
        Map<String, Object> details = Map.of(
            "principal", principal,
            "resource", event.getAuthorizationDecision().toString(),
            "timestamp", Instant.now()
        );
        
        auditLogger.warn("Authorization Denied: {}", details);
        
        recordComplianceEvent("ACCESS_DENIED", principal, details);
    }

    private void handleAuthenticationSuccess(AuditEvent event) {
        String principal = (String) event.getPrincipal();
        
        auditLogger.info("Authentication successful for principal: {}", principal);
        
        // Track successful Islamic banking logins for compliance
        recordIslamicBankingEvent("LOGIN_SUCCESS", principal, event.getData());
    }

    private void handleAuthenticationFailure(AuditEvent event) {
        String principal = (String) event.getPrincipal();
        
        auditLogger.warn("Authentication failed for principal: {} - Reason: {}", 
            principal, event.getData());
        
        // Check for brute force patterns
        checkForSuspiciousActivity(principal, "AUTHENTICATION_FAILURE");
        
        recordIslamicBankingEvent("LOGIN_FAILURE", principal, event.getData());
    }

    private void handleAccessDenied(AuditEvent event) {
        String principal = (String) event.getPrincipal();
        
        auditLogger.warn("Access denied for principal: {} - Resource: {}", 
            principal, event.getData());
        
        // Track unauthorized access attempts to financial resources
        if (isFinancialResource(event.getData())) {
            recordHighSeverityEvent("UNAUTHORIZED_FINANCIAL_ACCESS", principal, event.getData());
        }
        
        recordIslamicBankingEvent("ACCESS_DENIED", principal, event.getData());
    }

    private void handleRateLimitExceeded(AuditEvent event) {
        String principal = (String) event.getPrincipal();
        
        auditLogger.warn("Rate limit exceeded for principal: {} - Details: {}", 
            principal, event.getData());
        
        // Check for potential DDoS or abuse
        checkForAbusePattern(principal, "RATE_LIMIT_EXCEEDED");
        
        recordSecurityIncident("RATE_LIMIT_EXCEEDED", principal, event.getData());
    }

    private void handleDPoPValidationFailed(AuditEvent event) {
        String principal = (String) event.getPrincipal();
        
        auditLogger.error("DPoP validation failed for principal: {} - Reason: {}", 
            principal, event.getData());
        
        // DPoP failures are serious security events in FAPI 2.0
        recordHighSeverityEvent("DPOP_VALIDATION_FAILED", principal, event.getData());
        
        // Alert security team for potential token theft/replay attack
        alertSecurityTeam("DPoP validation failure detected", principal, event.getData());
    }

    private void handleHighValueTransaction(AuditEvent event) {
        String principal = (String) event.getPrincipal();
        
        auditLogger.info("High-value transaction attempt by principal: {} - Details: {}", 
            principal, event.getData());
        
        // All high-value transactions must be audited for AML compliance
        recordAmlEvent("HIGH_VALUE_TRANSACTION", principal, event.getData());
        
        // Check if proper enhanced authentication was used
        validateEnhancedAuthentication(principal, event.getData());
    }

    private void handleGenericSecurityEvent(AuditEvent event) {
        auditLogger.info("Security event: {} for principal: {} - Data: {}", 
            event.getType(), event.getPrincipal(), event.getData());
        
        recordComplianceEvent(event.getType(), (String) event.getPrincipal(), event.getData());
    }

    // Helper Methods

    private String extractClientId(org.springframework.security.core.Authentication authentication) {
        // Extract client_id from JWT claims or OAuth2 authentication
        // Implementation would depend on specific authentication mechanism
        return "extracted-client-id"; // Simplified for demo
    }

    private boolean isFinancialResource(Map<String, Object> data) {
        String resource = data.toString();
        return resource.contains("/payments/") || 
               resource.contains("/accounts/") || 
               resource.contains("/murabaha/") ||
               resource.contains("/high-value/");
    }

    private void checkForSuspiciousActivity(String principal, String eventType) {
        // Implementation would track patterns and alert on suspicious activity
        logger.info("Checking suspicious activity patterns for {} - {}", principal, eventType);
    }

    private void checkForAbusePattern(String principal, String eventType) {
        // Implementation would detect abuse patterns and potentially block clients
        logger.info("Checking abuse patterns for {} - {}", principal, eventType);
    }

    private void alertSecurityTeam(String message, String principal, Map<String, Object> data) {
        // Implementation would send alerts to security team via email/Slack/etc.
        logger.error("SECURITY ALERT: {} - Principal: {} - Data: {}", message, principal, data);
    }

    private void validateEnhancedAuthentication(String principal, Map<String, Object> data) {
        // Verify that high-value transactions used proper MFA/enhanced auth
        logger.info("Validating enhanced authentication for high-value transaction: {}", principal);
    }

    private void recordComplianceEvent(String eventType, String principal, Map<String, Object> data) {
        // Record for CBUAE/VARA regulatory compliance reporting
        auditLogger.info("COMPLIANCE_EVENT: {} - Principal: {} - Data: {}", eventType, principal, data);
    }

    private void recordIslamicBankingEvent(String eventType, String principal, Map<String, Object> data) {
        // Record Islamic banking specific events for Sharia compliance
        auditLogger.info("ISLAMIC_BANKING_EVENT: {} - Principal: {} - Data: {}", eventType, principal, data);
    }

    private void recordHighSeverityEvent(String eventType, String principal, Map<String, Object> data) {
        // Record high-severity security events requiring immediate attention
        auditLogger.error("HIGH_SEVERITY_EVENT: {} - Principal: {} - Data: {}", eventType, principal, data);
    }

    private void recordSecurityIncident(String eventType, String principal, Map<String, Object> data) {
        // Record security incidents for investigation and response
        auditLogger.warn("SECURITY_INCIDENT: {} - Principal: {} - Data: {}", eventType, principal, data);
    }

    private void recordAmlEvent(String eventType, String principal, Map<String, Object> data) {
        // Record AML-related events for compliance and reporting
        auditLogger.info("AML_EVENT: {} - Principal: {} - Data: {}", eventType, principal, data);
    }
}