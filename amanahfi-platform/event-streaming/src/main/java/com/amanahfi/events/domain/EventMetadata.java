package com.amanahfi.events.domain;

import java.time.Instant;

/**
 * Event Metadata for Islamic Banking Compliance and Audit
 * 
 * Enriches domain events with:
 * - Regulatory compliance information
 * - Islamic banking compliance flags
 * - Audit trail requirements
 * - Event processing metadata
 */
public class EventMetadata {
    
    private final String eventId;
    private final String eventType;
    private final String aggregateId;
    private final String aggregateType;
    private final Long version;
    private final Instant timestamp;
    private final String correlationId;
    private final String causationId;
    
    // Islamic Banking Compliance
    private final boolean islamicBankingCompliant;
    private final boolean shariahApproved;
    private final boolean cbdcCompliant;
    private final String regulatoryCompliance;
    private final String complianceType;
    
    // Audit and Monitoring
    private final boolean auditRequired;
    private final String auditLevel;
    private final String tenantId;
    private final String userId;
    private final String sessionId;
    
    private EventMetadata(Builder builder) {
        this.eventId = builder.eventId;
        this.eventType = builder.eventType;
        this.aggregateId = builder.aggregateId;
        this.aggregateType = builder.aggregateType;
        this.version = builder.version;
        this.timestamp = builder.timestamp;
        this.correlationId = builder.correlationId;
        this.causationId = builder.causationId;
        this.islamicBankingCompliant = builder.islamicBankingCompliant;
        this.shariahApproved = builder.shariahApproved;
        this.cbdcCompliant = builder.cbdcCompliant;
        this.regulatoryCompliance = builder.regulatoryCompliance;
        this.complianceType = builder.complianceType;
        this.auditRequired = builder.auditRequired;
        this.auditLevel = builder.auditLevel;
        this.tenantId = builder.tenantId;
        this.userId = builder.userId;
        this.sessionId = builder.sessionId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String eventId;
        private String eventType;
        private String aggregateId;
        private String aggregateType;
        private Long version;
        private Instant timestamp;
        private String correlationId;
        private String causationId;
        private boolean islamicBankingCompliant = false;
        private boolean shariahApproved = false;
        private boolean cbdcCompliant = false;
        private String regulatoryCompliance;
        private String complianceType;
        private boolean auditRequired = false;
        private String auditLevel;
        private String tenantId;
        private String userId;
        private String sessionId;

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder version(Long version) {
            this.version = version;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder causationId(String causationId) {
            this.causationId = causationId;
            return this;
        }

        public Builder islamicBankingCompliant(boolean islamicBankingCompliant) {
            this.islamicBankingCompliant = islamicBankingCompliant;
            return this;
        }

        public Builder shariahApproved(boolean shariahApproved) {
            this.shariahApproved = shariahApproved;
            return this;
        }

        public Builder cbdcCompliant(boolean cbdcCompliant) {
            this.cbdcCompliant = cbdcCompliant;
            return this;
        }

        public Builder regulatoryCompliance(String regulatoryCompliance) {
            this.regulatoryCompliance = regulatoryCompliance;
            return this;
        }

        public Builder complianceType(String complianceType) {
            this.complianceType = complianceType;
            return this;
        }

        public Builder auditRequired(boolean auditRequired) {
            this.auditRequired = auditRequired;
            return this;
        }

        public Builder auditLevel(String auditLevel) {
            this.auditLevel = auditLevel;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public EventMetadata build() {
            return new EventMetadata(this);
        }
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public String getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public Long getVersion() { return version; }
    public Instant getTimestamp() { return timestamp; }
    public String getCorrelationId() { return correlationId; }
    public String getCausationId() { return causationId; }
    public boolean isIslamicBankingCompliant() { return islamicBankingCompliant; }
    public boolean isShariahApproved() { return shariahApproved; }
    public boolean isCbdcCompliant() { return cbdcCompliant; }
    public String getRegulatoryCompliance() { return regulatoryCompliance; }
    public String getComplianceType() { return complianceType; }
    public boolean isAuditRequired() { return auditRequired; }
    public String getAuditLevel() { return auditLevel; }
    public String getTenantId() { return tenantId; }
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
}