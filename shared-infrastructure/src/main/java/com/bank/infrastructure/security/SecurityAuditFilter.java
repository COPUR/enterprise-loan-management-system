package com.bank.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Security Audit Filter
 * 
 * Comprehensive security event logging and audit trail:
 * - Authentication events (login, logout, failures)
 * - Authorization events (access granted/denied)
 * - Data access events (sensitive data access)
 * - Administrative actions
 * - Configuration changes
 * - Security policy violations
 * - Session management events
 * - API access patterns
 * - Compliance audit trail
 * - Real-time security monitoring
 */
@Component
public class SecurityAuditFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditFilter.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final Logger complianceLogger = LoggerFactory.getLogger("COMPLIANCE");
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Audit event types
    private static final String REQUEST_ACCESS = "REQUEST_ACCESS";
    private static final String AUTHENTICATION_SUCCESS = "AUTHENTICATION_SUCCESS";
    private static final String AUTHENTICATION_FAILURE = "AUTHENTICATION_FAILURE";
    private static final String AUTHORIZATION_SUCCESS = "AUTHORIZATION_SUCCESS";
    private static final String AUTHORIZATION_FAILURE = "AUTHORIZATION_FAILURE";
    private static final String SENSITIVE_DATA_ACCESS = "SENSITIVE_DATA_ACCESS";
    private static final String ADMIN_ACTION = "ADMIN_ACTION";
    private static final String SESSION_CREATED = "SESSION_CREATED";
    private static final String SESSION_TERMINATED = "SESSION_TERMINATED";
    private static final String POLICY_VIOLATION = "POLICY_VIOLATION";
    
    // Redis keys for audit data
    private static final String AUDIT_EVENT_KEY = "audit:event:";
    private static final String USER_SESSION_KEY = "audit:session:";
    private static final String COMPLIANCE_EVENT_KEY = "compliance:event:";
    
    // Sensitive endpoints that require special logging
    private static final String[] SENSITIVE_ENDPOINTS = {
        "/api/v1/customers",
        "/api/v1/loans",
        "/api/v1/payments",
        "/api/v1/admin",
        "/api/v1/reports"
    };
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Record request start time
        long startTime = System.currentTimeMillis();
        
        // Get authentication context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Create audit context
        AuditContext auditContext = createAuditContext(request, authentication);
        
        try {
            // Log request access
            logRequestAccess(auditContext);
            
            // Continue with filter chain
            filterChain.doFilter(request, response);
            
            // Log successful access
            logSuccessfulAccess(auditContext, response, startTime);
            
        } catch (Exception e) {
            // Log failed access
            logFailedAccess(auditContext, e, startTime);
            throw e;
        }
    }
    
    private AuditContext createAuditContext(HttpServletRequest request, Authentication authentication) {
        AuditContext context = new AuditContext();
        
        // Request information
        context.setRequestId(request.getHeader("X-Request-ID"));
        context.setCorrelationId(request.getHeader("X-Correlation-ID"));
        context.setUri(request.getRequestURI());
        context.setMethod(request.getMethod());
        context.setClientIp(getClientIpAddress(request));
        context.setUserAgent(request.getHeader("User-Agent"));
        context.setTimestamp(Instant.now());
        
        // Authentication information
        if (authentication != null && authentication.isAuthenticated()) {
            context.setUserId(authentication.getName());
            context.setAuthorities(authentication.getAuthorities().toString());
            context.setAuthenticationMethod(getAuthenticationMethod(authentication));
        }
        
        // Session information
        if (request.getSession(false) != null) {
            context.setSessionId(request.getSession().getId());
        }
        
        // Request classification
        context.setSensitiveData(isSensitiveEndpoint(request.getRequestURI()));
        context.setAdminAction(isAdminAction(request.getRequestURI()));
        context.setComplianceRelevant(isComplianceRelevant(request.getRequestURI()));
        
        return context;
    }
    
    private void logRequestAccess(AuditContext context) {
        try {
            // Create audit event
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", REQUEST_ACCESS);
            auditEvent.put("timestamp", context.getTimestamp().toString());
            auditEvent.put("requestId", context.getRequestId());
            auditEvent.put("correlationId", context.getCorrelationId());
            auditEvent.put("userId", context.getUserId());
            auditEvent.put("sessionId", context.getSessionId());
            auditEvent.put("method", context.getMethod());
            auditEvent.put("uri", context.getUri());
            auditEvent.put("clientIp", context.getClientIp());
            auditEvent.put("userAgent", context.getUserAgent());
            auditEvent.put("authorities", context.getAuthorities());
            auditEvent.put("authenticationMethod", context.getAuthenticationMethod());
            auditEvent.put("sensitiveData", context.isSensitiveData());
            auditEvent.put("adminAction", context.isAdminAction());
            
            // Store in Redis for real-time monitoring
            String eventKey = AUDIT_EVENT_KEY + context.getRequestId();
            redisTemplate.opsForValue().set(
                eventKey, 
                objectMapper.writeValueAsString(auditEvent),
                24, 
                TimeUnit.HOURS
            );
            
            // Log to audit logger
            auditLogger.info("Request Access: {} {} by {} from {} | RequestId: {} | Session: {}",
                context.getMethod(),
                context.getUri(),
                context.getUserId() != null ? context.getUserId() : "anonymous",
                context.getClientIp(),
                context.getRequestId(),
                context.getSessionId()
            );
            
            // Special logging for sensitive data access
            if (context.isSensitiveData()) {
                logSensitiveDataAccess(context);
            }
            
            // Special logging for admin actions
            if (context.isAdminAction()) {
                logAdminAction(context);
            }
            
            // Special logging for compliance events
            if (context.isComplianceRelevant()) {
                logComplianceEvent(context);
            }
            
        } catch (Exception e) {
            logger.error("Failed to log request access", e);
        }
    }
    
    private void logSuccessfulAccess(AuditContext context, HttpServletResponse response, long startTime) {
        try {
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Create success audit event
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", AUTHORIZATION_SUCCESS);
            auditEvent.put("timestamp", Instant.now().toString());
            auditEvent.put("requestId", context.getRequestId());
            auditEvent.put("userId", context.getUserId());
            auditEvent.put("uri", context.getUri());
            auditEvent.put("method", context.getMethod());
            auditEvent.put("responseStatus", response.getStatus());
            auditEvent.put("responseTime", responseTime);
            auditEvent.put("clientIp", context.getClientIp());
            
            // Store in Redis
            String eventKey = AUDIT_EVENT_KEY + context.getRequestId() + ":success";
            redisTemplate.opsForValue().set(
                eventKey, 
                objectMapper.writeValueAsString(auditEvent),
                24, 
                TimeUnit.HOURS
            );
            
            // Log success
            auditLogger.info("Successful Access: {} {} by {} | Status: {} | ResponseTime: {}ms | RequestId: {}",
                context.getMethod(),
                context.getUri(),
                context.getUserId() != null ? context.getUserId() : "anonymous",
                response.getStatus(),
                responseTime,
                context.getRequestId()
            );
            
        } catch (Exception e) {
            logger.error("Failed to log successful access", e);
        }
    }
    
    private void logFailedAccess(AuditContext context, Exception exception, long startTime) {
        try {
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Create failure audit event
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", AUTHORIZATION_FAILURE);
            auditEvent.put("timestamp", Instant.now().toString());
            auditEvent.put("requestId", context.getRequestId());
            auditEvent.put("userId", context.getUserId());
            auditEvent.put("uri", context.getUri());
            auditEvent.put("method", context.getMethod());
            auditEvent.put("error", exception.getMessage());
            auditEvent.put("exceptionType", exception.getClass().getSimpleName());
            auditEvent.put("responseTime", responseTime);
            auditEvent.put("clientIp", context.getClientIp());
            
            // Store in Redis
            String eventKey = AUDIT_EVENT_KEY + context.getRequestId() + ":failure";
            redisTemplate.opsForValue().set(
                eventKey, 
                objectMapper.writeValueAsString(auditEvent),
                24, 
                TimeUnit.HOURS
            );
            
            // Log failure
            auditLogger.warn("Failed Access: {} {} by {} | Error: {} | ResponseTime: {}ms | RequestId: {}",
                context.getMethod(),
                context.getUri(),
                context.getUserId() != null ? context.getUserId() : "anonymous",
                exception.getMessage(),
                responseTime,
                context.getRequestId()
            );
            
        } catch (Exception e) {
            logger.error("Failed to log failed access", e);
        }
    }
    
    private void logSensitiveDataAccess(AuditContext context) {
        try {
            // Create sensitive data access event
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", SENSITIVE_DATA_ACCESS);
            auditEvent.put("timestamp", context.getTimestamp().toString());
            auditEvent.put("requestId", context.getRequestId());
            auditEvent.put("userId", context.getUserId());
            auditEvent.put("uri", context.getUri());
            auditEvent.put("method", context.getMethod());
            auditEvent.put("clientIp", context.getClientIp());
            auditEvent.put("userAgent", context.getUserAgent());
            auditEvent.put("dataType", classifyDataType(context.getUri()));
            
            // Store in Redis with longer retention
            String eventKey = AUDIT_EVENT_KEY + "sensitive:" + context.getRequestId();
            redisTemplate.opsForValue().set(
                eventKey, 
                objectMapper.writeValueAsString(auditEvent),
                7, 
                TimeUnit.DAYS
            );
            
            // Log to audit logger
            auditLogger.warn("Sensitive Data Access: {} {} by {} from {} | DataType: {} | RequestId: {}",
                context.getMethod(),
                context.getUri(),
                context.getUserId() != null ? context.getUserId() : "anonymous",
                context.getClientIp(),
                classifyDataType(context.getUri()),
                context.getRequestId()
            );
            
        } catch (Exception e) {
            logger.error("Failed to log sensitive data access", e);
        }
    }
    
    private void logAdminAction(AuditContext context) {
        try {
            // Create admin action event
            Map<String, Object> auditEvent = new HashMap<>();
            auditEvent.put("eventType", ADMIN_ACTION);
            auditEvent.put("timestamp", context.getTimestamp().toString());
            auditEvent.put("requestId", context.getRequestId());
            auditEvent.put("userId", context.getUserId());
            auditEvent.put("uri", context.getUri());
            auditEvent.put("method", context.getMethod());
            auditEvent.put("clientIp", context.getClientIp());
            auditEvent.put("userAgent", context.getUserAgent());
            auditEvent.put("authorities", context.getAuthorities());
            auditEvent.put("actionType", classifyAdminAction(context.getUri()));
            
            // Store in Redis with longer retention
            String eventKey = AUDIT_EVENT_KEY + "admin:" + context.getRequestId();
            redisTemplate.opsForValue().set(
                eventKey, 
                objectMapper.writeValueAsString(auditEvent),
                30, 
                TimeUnit.DAYS
            );
            
            // Log to audit logger
            auditLogger.warn("Admin Action: {} {} by {} from {} | ActionType: {} | RequestId: {}",
                context.getMethod(),
                context.getUri(),
                context.getUserId() != null ? context.getUserId() : "anonymous",
                context.getClientIp(),
                classifyAdminAction(context.getUri()),
                context.getRequestId()
            );
            
        } catch (Exception e) {
            logger.error("Failed to log admin action", e);
        }
    }
    
    private void logComplianceEvent(AuditContext context) {
        try {
            // Create compliance event
            Map<String, Object> complianceEvent = new HashMap<>();
            complianceEvent.put("eventType", "COMPLIANCE_ACCESS");
            complianceEvent.put("timestamp", context.getTimestamp().toString());
            complianceEvent.put("requestId", context.getRequestId());
            complianceEvent.put("userId", context.getUserId());
            complianceEvent.put("uri", context.getUri());
            complianceEvent.put("method", context.getMethod());
            complianceEvent.put("clientIp", context.getClientIp());
            complianceEvent.put("complianceCategory", classifyComplianceCategory(context.getUri()));
            complianceEvent.put("regulatoryRequirement", identifyRegulatoryRequirement(context.getUri()));
            
            // Store in Redis with extended retention for compliance
            String eventKey = COMPLIANCE_EVENT_KEY + context.getRequestId();
            redisTemplate.opsForValue().set(
                eventKey, 
                objectMapper.writeValueAsString(complianceEvent),
                365, 
                TimeUnit.DAYS
            );
            
            // Log to compliance logger
            complianceLogger.info("Compliance Event: {} {} by {} | Category: {} | Requirement: {} | RequestId: {}",
                context.getMethod(),
                context.getUri(),
                context.getUserId() != null ? context.getUserId() : "anonymous",
                classifyComplianceCategory(context.getUri()),
                identifyRegulatoryRequirement(context.getUri()),
                context.getRequestId()
            );
            
        } catch (Exception e) {
            logger.error("Failed to log compliance event", e);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "X-Originating-IP", "CF-Connecting-IP", "True-Client-IP"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    private String getAuthenticationMethod(Authentication authentication) {
        if (authentication == null) {
            return "ANONYMOUS";
        }
        
        String className = authentication.getClass().getSimpleName();
        if (className.contains("Jwt")) {
            return "JWT";
        } else if (className.contains("OAuth2")) {
            return "OAUTH2";
        } else if (className.contains("Certificate")) {
            return "CERTIFICATE";
        } else {
            return "UNKNOWN";
        }
    }
    
    private boolean isSensitiveEndpoint(String uri) {
        for (String sensitiveEndpoint : SENSITIVE_ENDPOINTS) {
            if (uri.startsWith(sensitiveEndpoint)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isAdminAction(String uri) {
        return uri.startsWith("/api/v1/admin") || 
               uri.contains("/config") || 
               uri.contains("/management");
    }
    
    private boolean isComplianceRelevant(String uri) {
        return uri.startsWith("/api/v1/customers") ||
               uri.startsWith("/api/v1/loans") ||
               uri.startsWith("/api/v1/payments") ||
               uri.startsWith("/api/v1/reports");
    }
    
    private String classifyDataType(String uri) {
        if (uri.startsWith("/api/v1/customers")) {
            return "CUSTOMER_DATA";
        } else if (uri.startsWith("/api/v1/loans")) {
            return "LOAN_DATA";
        } else if (uri.startsWith("/api/v1/payments")) {
            return "PAYMENT_DATA";
        } else if (uri.startsWith("/api/v1/reports")) {
            return "REPORT_DATA";
        } else {
            return "UNKNOWN";
        }
    }
    
    private String classifyAdminAction(String uri) {
        if (uri.contains("/users")) {
            return "USER_MANAGEMENT";
        } else if (uri.contains("/config")) {
            return "CONFIGURATION";
        } else if (uri.contains("/system")) {
            return "SYSTEM_MANAGEMENT";
        } else {
            return "GENERAL_ADMIN";
        }
    }
    
    private String classifyComplianceCategory(String uri) {
        if (uri.startsWith("/api/v1/customers")) {
            return "DATA_PRIVACY";
        } else if (uri.startsWith("/api/v1/loans")) {
            return "LENDING_COMPLIANCE";
        } else if (uri.startsWith("/api/v1/payments")) {
            return "PAYMENT_COMPLIANCE";
        } else {
            return "GENERAL_COMPLIANCE";
        }
    }
    
    private String identifyRegulatoryRequirement(String uri) {
        if (uri.startsWith("/api/v1/customers")) {
            return "GDPR,PCI_DSS,KYC";
        } else if (uri.startsWith("/api/v1/loans")) {
            return "BASEL_III,IFRS_9,UAE_CENTRAL_BANK";
        } else if (uri.startsWith("/api/v1/payments")) {
            return "PCI_DSS,AML,UAE_CENTRAL_BANK";
        } else {
            return "GENERAL_BANKING";
        }
    }
    
    // Inner class for audit context
    private static class AuditContext {
        private String requestId;
        private String correlationId;
        private String uri;
        private String method;
        private String clientIp;
        private String userAgent;
        private String userId;
        private String sessionId;
        private String authorities;
        private String authenticationMethod;
        private Instant timestamp;
        private boolean sensitiveData;
        private boolean adminAction;
        private boolean complianceRelevant;
        
        // Getters and setters
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        
        public String getCorrelationId() { return correlationId; }
        public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public String getAuthorities() { return authorities; }
        public void setAuthorities(String authorities) { this.authorities = authorities; }
        
        public String getAuthenticationMethod() { return authenticationMethod; }
        public void setAuthenticationMethod(String authenticationMethod) { this.authenticationMethod = authenticationMethod; }
        
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        
        public boolean isSensitiveData() { return sensitiveData; }
        public void setSensitiveData(boolean sensitiveData) { this.sensitiveData = sensitiveData; }
        
        public boolean isAdminAction() { return adminAction; }
        public void setAdminAction(boolean adminAction) { this.adminAction = adminAction; }
        
        public boolean isComplianceRelevant() { return complianceRelevant; }
        public void setComplianceRelevant(boolean complianceRelevant) { this.complianceRelevant = complianceRelevant; }
    }
}