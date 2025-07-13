package com.bank.infrastructure.audit;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Event for Compliance and Security Logging
 * 
 * Captures all significant events in the banking platform for:
 * - Regulatory compliance (SOX, PCI-DSS, GDPR)
 * - Security monitoring and incident response
 * - Business activity tracking
 * - Forensic analysis
 */
public class AuditEvent {
    
    private final String eventId;
    private final String eventType;
    private final EventCategory category;
    private final EventSeverity severity;
    private final String userId;
    private final String customerId;
    private final String sessionId;
    private final String ipAddress;
    private final String userAgent;
    private final String resource;
    private final String action;
    private final ActionResult result;
    private final String description;
    private final Map<String, Object> metadata;
    private final LocalDateTime timestamp;
    private final String correlationId;
    private final String applicationName;
    private final String applicationVersion;
    
    public enum EventCategory {
        AUTHENTICATION,
        AUTHORIZATION,
        DATA_ACCESS,
        DATA_MODIFICATION,
        TRANSACTION,
        CONFIGURATION,
        SECURITY,
        COMPLIANCE,
        SYSTEM,
        FRAUD_DETECTION,
        CUSTOMER_ACTION,
        ADMINISTRATIVE
    }
    
    public enum EventSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    public enum ActionResult {
        SUCCESS,
        FAILURE,
        PARTIAL,
        BLOCKED,
        PENDING
    }
    
    private AuditEvent(Builder builder) {
        this.eventId = builder.eventId != null ? builder.eventId : UUID.randomUUID().toString();
        this.eventType = builder.eventType;
        this.category = builder.category;
        this.severity = builder.severity;
        this.userId = builder.userId;
        this.customerId = builder.customerId;
        this.sessionId = builder.sessionId;
        this.ipAddress = builder.ipAddress;
        this.userAgent = builder.userAgent;
        this.resource = builder.resource;
        this.action = builder.action;
        this.result = builder.result;
        this.description = builder.description;
        this.metadata = builder.metadata;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.correlationId = builder.correlationId;
        this.applicationName = builder.applicationName;
        this.applicationVersion = builder.applicationVersion;
    }
    
    // Getters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public EventCategory getCategory() { return category; }
    public EventSeverity getSeverity() { return severity; }
    public String getUserId() { return userId; }
    public String getCustomerId() { return customerId; }
    public String getSessionId() { return sessionId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public ActionResult getResult() { return result; }
    public String getDescription() { return description; }
    public Map<String, Object> getMetadata() { return metadata; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getApplicationName() { return applicationName; }
    public String getApplicationVersion() { return applicationVersion; }
    
    /**
     * Convert to structured log format for compliance
     */
    public String toStructuredLog() {
        return String.format(
            "AUDIT_EVENT: eventId=%s, eventType=%s, category=%s, severity=%s, " +
            "userId=%s, customerId=%s, resource=%s, action=%s, result=%s, " +
            "ipAddress=%s, timestamp=%s, correlationId=%s",
            eventId, eventType, category, severity,
            userId, customerId, resource, action, result,
            ipAddress, timestamp, correlationId
        );
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String eventId;
        private String eventType;
        private EventCategory category;
        private EventSeverity severity = EventSeverity.INFO;
        private String userId;
        private String customerId;
        private String sessionId;
        private String ipAddress;
        private String userAgent;
        private String resource;
        private String action;
        private ActionResult result = ActionResult.SUCCESS;
        private String description;
        private Map<String, Object> metadata;
        private LocalDateTime timestamp;
        private String correlationId;
        private String applicationName;
        private String applicationVersion;
        
        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public Builder category(EventCategory category) {
            this.category = category;
            return this;
        }
        
        public Builder severity(EventSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }
        
        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }
        
        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }
        
        public Builder action(String action) {
            this.action = action;
            return this;
        }
        
        public Builder result(ActionResult result) {
            this.result = result;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }
        
        public Builder applicationVersion(String applicationVersion) {
            this.applicationVersion = applicationVersion;
            return this;
        }
        
        public AuditEvent build() {
            return new AuditEvent(this);
        }
    }
}