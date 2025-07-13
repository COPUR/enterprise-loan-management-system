package com.amanahfi.compliance.domain.check;

import java.time.LocalDateTime;

public class ComplianceCheckCreatedEvent {
    private final String checkId;
    private final String entityId;
    private final ComplianceType complianceType;
    private final LocalDateTime timestamp;

    public ComplianceCheckCreatedEvent(String checkId, String entityId, ComplianceType complianceType) {
        this.checkId = checkId;
        this.entityId = entityId;
        this.complianceType = complianceType;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public String getEntityId() { return entityId; }
    public ComplianceType getComplianceType() { return complianceType; }
    public LocalDateTime getTimestamp() { return timestamp; }
}