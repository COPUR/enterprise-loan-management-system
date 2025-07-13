package com.bank.infrastructure.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit Event JPA Entity
 * 
 * Persistence mapping for audit events in the banking platform
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_user_time", columnList = "userId, timestamp"),
    @Index(name = "idx_audit_customer_time", columnList = "customerId, timestamp"),
    @Index(name = "idx_audit_category_time", columnList = "category, timestamp"),
    @Index(name = "idx_audit_correlation", columnList = "correlationId"),
    @Index(name = "idx_audit_resource", columnList = "resource"),
    @Index(name = "idx_audit_severity", columnList = "severity")
})
public class AuditEventEntity {
    
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private AuditEvent.EventCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AuditEvent.EventSeverity severity;
    
    @Column(name = "user_id", length = 100)
    private String userId;
    
    @Column(name = "customer_id", length = 100)
    private String customerId;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "resource", length = 255)
    private String resource;
    
    @Column(name = "action", length = 100)
    private String action;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private AuditEvent.ActionResult result;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    
    @Column(name = "application_name", length = 100)
    private String applicationName;
    
    @Column(name = "application_version", length = 50)
    private String applicationVersion;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public AuditEventEntity() {
        this.createdAt = LocalDateTime.now();
    }
    
    public AuditEventEntity(AuditEvent auditEvent) {
        this.eventId = auditEvent.getEventId();
        this.eventType = auditEvent.getEventType();
        this.category = auditEvent.getCategory();
        this.severity = auditEvent.getSeverity();
        this.userId = auditEvent.getUserId();
        this.customerId = auditEvent.getCustomerId();
        this.sessionId = auditEvent.getSessionId();
        this.ipAddress = auditEvent.getIpAddress();
        this.userAgent = auditEvent.getUserAgent();
        this.resource = auditEvent.getResource();
        this.action = auditEvent.getAction();
        this.result = auditEvent.getResult();
        this.description = auditEvent.getDescription();
        this.metadata = convertMetadataToJson(auditEvent.getMetadata());
        this.timestamp = auditEvent.getTimestamp();
        this.correlationId = auditEvent.getCorrelationId();
        this.applicationName = auditEvent.getApplicationName();
        this.applicationVersion = auditEvent.getApplicationVersion();
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Convert to domain object
     */
    public AuditEvent toDomainObject() {
        return AuditEvent.builder()
            .eventId(eventId)
            .eventType(eventType)
            .category(category)
            .severity(severity)
            .userId(userId)
            .customerId(customerId)
            .sessionId(sessionId)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .resource(resource)
            .action(action)
            .result(result)
            .description(description)
            .metadata(convertJsonToMetadata(metadata))
            .timestamp(timestamp)
            .correlationId(correlationId)
            .applicationName(applicationName)
            .applicationVersion(applicationVersion)
            .build();
    }
    
    // Lifecycle callbacks
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper methods for metadata conversion
    private String convertMetadataToJson(java.util.Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        
        // Simple JSON conversion - in production, use proper JSON library
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (java.util.Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append("\"").append(String.valueOf(entry.getValue())).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    private java.util.Map<String, Object> convertJsonToMetadata(String json) {
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return metadata;
        }
        
        // Simple JSON parsing - in production, use proper JSON library
        // This is a simplified implementation for demonstration
        return metadata;
    }
    
    // Getters and setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public AuditEvent.EventCategory getCategory() { return category; }
    public void setCategory(AuditEvent.EventCategory category) { this.category = category; }
    
    public AuditEvent.EventSeverity getSeverity() { return severity; }
    public void setSeverity(AuditEvent.EventSeverity severity) { this.severity = severity; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public AuditEvent.ActionResult getResult() { return result; }
    public void setResult(AuditEvent.ActionResult result) { this.result = result; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    
    public String getApplicationVersion() { return applicationVersion; }
    public void setApplicationVersion(String applicationVersion) { this.applicationVersion = applicationVersion; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}