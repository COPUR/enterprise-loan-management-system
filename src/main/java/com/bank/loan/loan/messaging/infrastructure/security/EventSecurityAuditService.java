package com.bank.loanmanagement.loan.messaging.infrastructure.security;

import com.bank.loanmanagement.loan.sharedkernel.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event Security Audit Service
 * Provides comprehensive auditing for event security operations
 * Ensures compliance with banking regulations and FAPI standards
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventSecurityAuditService {

    private final Map<String, AuditEntry> auditLog = new ConcurrentHashMap<>();
    
    /**
     * Audit event publication
     */
    public void auditEventPublication(DomainEvent event, String topic, boolean success, String errorMessage) {
        try {
            AuditEntry auditEntry = AuditEntry.builder()
                .auditId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .action("EVENT_PUBLICATION")
                .eventType(event.getEventType())
                .eventId(event.getEventId())
                .aggregateId(event.getAggregateId())
                .topic(topic)
                .success(success)
                .errorMessage(errorMessage)
                .fapiInteractionId((String) event.getMetadata().get("fapiInteractionId"))
                .clientId((String) event.getMetadata().get("clientId"))
                .customerIpAddress((String) event.getMetadata().get("customerIpAddress"))
                .isCriticalEvent((Boolean) event.getMetadata().getOrDefault("criticalBankingEvent", false))
                .encryptionApplied((Boolean) event.getMetadata().getOrDefault("requiresEncryption", false))
                .auditLevel((String) event.getMetadata().getOrDefault("auditLevel", "STANDARD"))
                .build();
            
            // Store audit entry
            auditLog.put(auditEntry.getAuditId(), auditEntry);
            
            // Log for external audit systems
            if (auditEntry.isCriticalEvent()) {
                log.warn("CRITICAL_EVENT_AUDIT: {}", auditEntry.toAuditString());
            } else {
                log.info("EVENT_AUDIT: {}", auditEntry.toAuditString());
            }
            
        } catch (Exception e) {
            log.error("Failed to audit event publication for event: {}", event.getEventType(), e);
        }
    }
    
    /**
     * Audit event consumption
     */
    public void auditEventConsumption(DomainEvent event, String topic, boolean success, String errorMessage) {
        try {
            AuditEntry auditEntry = AuditEntry.builder()
                .auditId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .action("EVENT_CONSUMPTION")
                .eventType(event.getEventType())
                .eventId(event.getEventId())
                .aggregateId(event.getAggregateId())
                .topic(topic)
                .success(success)
                .errorMessage(errorMessage)
                .fapiInteractionId((String) event.getMetadata().get("fapiInteractionId"))
                .clientId((String) event.getMetadata().get("clientId"))
                .customerIpAddress((String) event.getMetadata().get("customerIpAddress"))
                .isCriticalEvent((Boolean) event.getMetadata().getOrDefault("criticalBankingEvent", false))
                .encryptionApplied((Boolean) event.getMetadata().getOrDefault("requiresEncryption", false))
                .auditLevel((String) event.getMetadata().getOrDefault("auditLevel", "STANDARD"))
                .build();
            
            // Store audit entry
            auditLog.put(auditEntry.getAuditId(), auditEntry);
            
            // Log for external audit systems
            if (auditEntry.isCriticalEvent()) {
                log.warn("CRITICAL_EVENT_CONSUMPTION_AUDIT: {}", auditEntry.toAuditString());
            } else {
                log.info("EVENT_CONSUMPTION_AUDIT: {}", auditEntry.toAuditString());
            }
            
        } catch (Exception e) {
            log.error("Failed to audit event consumption for event: {}", event.getEventType(), e);
        }
    }
    
    /**
     * Audit security validation failure
     */
    public void auditSecurityValidationFailure(String eventType, String reason, String fapiInteractionId) {
        try {
            AuditEntry auditEntry = AuditEntry.builder()
                .auditId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .action("SECURITY_VALIDATION_FAILURE")
                .eventType(eventType)
                .success(false)
                .errorMessage(reason)
                .fapiInteractionId(fapiInteractionId)
                .isCriticalEvent(true)
                .auditLevel("HIGH")
                .build();
            
            // Store audit entry
            auditLog.put(auditEntry.getAuditId(), auditEntry);
            
            // Log as security incident
            log.error("SECURITY_VALIDATION_FAILURE: {}", auditEntry.toAuditString());
            
        } catch (Exception e) {
            log.error("Failed to audit security validation failure", e);
        }
    }
    
    /**
     * Audit FAPI compliance violation
     */
    public void auditFAPIComplianceViolation(String eventType, String violationType, String details, String fapiInteractionId) {
        try {
            AuditEntry auditEntry = AuditEntry.builder()
                .auditId(UUID.randomUUID().toString())
                .timestamp(OffsetDateTime.now())
                .action("FAPI_COMPLIANCE_VIOLATION")
                .eventType(eventType)
                .success(false)
                .errorMessage(String.format("Violation: %s - %s", violationType, details))
                .fapiInteractionId(fapiInteractionId)
                .isCriticalEvent(true)
                .auditLevel("CRITICAL")
                .build();
            
            // Store audit entry
            auditLog.put(auditEntry.getAuditId(), auditEntry);
            
            // Log as critical security incident
            log.error("FAPI_COMPLIANCE_VIOLATION: {}", auditEntry.toAuditString());
            
        } catch (Exception e) {
            log.error("Failed to audit FAPI compliance violation", e);
        }
    }
    
    /**
     * Get audit entries for reporting
     */
    public java.util.List<AuditEntry> getAuditEntries(OffsetDateTime from, OffsetDateTime to) {
        return auditLog.values().stream()
            .filter(entry -> entry.getTimestamp().isAfter(from) && entry.getTimestamp().isBefore(to))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get critical audit entries
     */
    public java.util.List<AuditEntry> getCriticalAuditEntries(OffsetDateTime from, OffsetDateTime to) {
        return auditLog.values().stream()
            .filter(entry -> entry.isCriticalEvent())
            .filter(entry -> entry.getTimestamp().isAfter(from) && entry.getTimestamp().isBefore(to))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get audit statistics
     */
    public AuditStatistics getAuditStatistics(OffsetDateTime from, OffsetDateTime to) {
        var entries = getAuditEntries(from, to);
        
        long totalEvents = entries.size();
        long successfulEvents = entries.stream().mapToLong(e -> e.isSuccess() ? 1 : 0).sum();
        long failedEvents = totalEvents - successfulEvents;
        long criticalEvents = entries.stream().mapToLong(e -> e.isCriticalEvent() ? 1 : 0).sum();
        long securityViolations = entries.stream()
            .mapToLong(e -> e.getAction().contains("VIOLATION") || e.getAction().contains("FAILURE") ? 1 : 0)
            .sum();
        
        return AuditStatistics.builder()
            .periodFrom(from)
            .periodTo(to)
            .totalEvents(totalEvents)
            .successfulEvents(successfulEvents)
            .failedEvents(failedEvents)
            .criticalEvents(criticalEvents)
            .securityViolations(securityViolations)
            .successRate(totalEvents > 0 ? (double) successfulEvents / totalEvents * 100 : 0)
            .build();
    }
    
    /**
     * Audit Entry data structure
     */
    public static class AuditEntry {
        private final String auditId;
        private final OffsetDateTime timestamp;
        private final String action;
        private final String eventType;
        private final String eventId;
        private final String aggregateId;
        private final String topic;
        private final boolean success;
        private final String errorMessage;
        private final String fapiInteractionId;
        private final String clientId;
        private final String customerIpAddress;
        private final boolean isCriticalEvent;
        private final boolean encryptionApplied;
        private final String auditLevel;
        
        private AuditEntry(Builder builder) {
            this.auditId = builder.auditId;
            this.timestamp = builder.timestamp;
            this.action = builder.action;
            this.eventType = builder.eventType;
            this.eventId = builder.eventId;
            this.aggregateId = builder.aggregateId;
            this.topic = builder.topic;
            this.success = builder.success;
            this.errorMessage = builder.errorMessage;
            this.fapiInteractionId = builder.fapiInteractionId;
            this.clientId = builder.clientId;
            this.customerIpAddress = builder.customerIpAddress;
            this.isCriticalEvent = builder.isCriticalEvent;
            this.encryptionApplied = builder.encryptionApplied;
            this.auditLevel = builder.auditLevel;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public String toAuditString() {
            return String.format(
                "auditId=%s, timestamp=%s, action=%s, eventType=%s, eventId=%s, topic=%s, success=%s, " +
                "fapiInteractionId=%s, clientId=%s, critical=%s, encryption=%s, auditLevel=%s, error=%s",
                auditId, timestamp, action, eventType, eventId, topic, success,
                fapiInteractionId, clientId, isCriticalEvent, encryptionApplied, auditLevel, errorMessage
            );
        }
        
        // Getters
        public String getAuditId() { return auditId; }
        public OffsetDateTime getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public String getEventType() { return eventType; }
        public String getEventId() { return eventId; }
        public String getAggregateId() { return aggregateId; }
        public String getTopic() { return topic; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public String getFapiInteractionId() { return fapiInteractionId; }
        public String getClientId() { return clientId; }
        public String getCustomerIpAddress() { return customerIpAddress; }
        public boolean isCriticalEvent() { return isCriticalEvent; }
        public boolean isEncryptionApplied() { return encryptionApplied; }
        public String getAuditLevel() { return auditLevel; }
        
        public static class Builder {
            private String auditId;
            private OffsetDateTime timestamp;
            private String action;
            private String eventType;
            private String eventId;
            private String aggregateId;
            private String topic;
            private boolean success;
            private String errorMessage;
            private String fapiInteractionId;
            private String clientId;
            private String customerIpAddress;
            private boolean isCriticalEvent;
            private boolean encryptionApplied;
            private String auditLevel;
            
            public Builder auditId(String auditId) { this.auditId = auditId; return this; }
            public Builder timestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; return this; }
            public Builder action(String action) { this.action = action; return this; }
            public Builder eventType(String eventType) { this.eventType = eventType; return this; }
            public Builder eventId(String eventId) { this.eventId = eventId; return this; }
            public Builder aggregateId(String aggregateId) { this.aggregateId = aggregateId; return this; }
            public Builder topic(String topic) { this.topic = topic; return this; }
            public Builder success(boolean success) { this.success = success; return this; }
            public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
            public Builder fapiInteractionId(String fapiInteractionId) { this.fapiInteractionId = fapiInteractionId; return this; }
            public Builder clientId(String clientId) { this.clientId = clientId; return this; }
            public Builder customerIpAddress(String customerIpAddress) { this.customerIpAddress = customerIpAddress; return this; }
            public Builder isCriticalEvent(boolean isCriticalEvent) { this.isCriticalEvent = isCriticalEvent; return this; }
            public Builder encryptionApplied(boolean encryptionApplied) { this.encryptionApplied = encryptionApplied; return this; }
            public Builder auditLevel(String auditLevel) { this.auditLevel = auditLevel; return this; }
            
            public AuditEntry build() {
                return new AuditEntry(this);
            }
        }
    }
    
    /**
     * Audit Statistics data structure
     */
    public static class AuditStatistics {
        private final OffsetDateTime periodFrom;
        private final OffsetDateTime periodTo;
        private final long totalEvents;
        private final long successfulEvents;
        private final long failedEvents;
        private final long criticalEvents;
        private final long securityViolations;
        private final double successRate;
        
        private AuditStatistics(Builder builder) {
            this.periodFrom = builder.periodFrom;
            this.periodTo = builder.periodTo;
            this.totalEvents = builder.totalEvents;
            this.successfulEvents = builder.successfulEvents;
            this.failedEvents = builder.failedEvents;
            this.criticalEvents = builder.criticalEvents;
            this.securityViolations = builder.securityViolations;
            this.successRate = builder.successRate;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public OffsetDateTime getPeriodFrom() { return periodFrom; }
        public OffsetDateTime getPeriodTo() { return periodTo; }
        public long getTotalEvents() { return totalEvents; }
        public long getSuccessfulEvents() { return successfulEvents; }
        public long getFailedEvents() { return failedEvents; }
        public long getCriticalEvents() { return criticalEvents; }
        public long getSecurityViolations() { return securityViolations; }
        public double getSuccessRate() { return successRate; }
        
        public static class Builder {
            private OffsetDateTime periodFrom;
            private OffsetDateTime periodTo;
            private long totalEvents;
            private long successfulEvents;
            private long failedEvents;
            private long criticalEvents;
            private long securityViolations;
            private double successRate;
            
            public Builder periodFrom(OffsetDateTime periodFrom) { this.periodFrom = periodFrom; return this; }
            public Builder periodTo(OffsetDateTime periodTo) { this.periodTo = periodTo; return this; }
            public Builder totalEvents(long totalEvents) { this.totalEvents = totalEvents; return this; }
            public Builder successfulEvents(long successfulEvents) { this.successfulEvents = successfulEvents; return this; }
            public Builder failedEvents(long failedEvents) { this.failedEvents = failedEvents; return this; }
            public Builder criticalEvents(long criticalEvents) { this.criticalEvents = criticalEvents; return this; }
            public Builder securityViolations(long securityViolations) { this.securityViolations = securityViolations; return this; }
            public Builder successRate(double successRate) { this.successRate = successRate; return this; }
            
            public AuditStatistics build() {
                return new AuditStatistics(this);
            }
        }
    }
}