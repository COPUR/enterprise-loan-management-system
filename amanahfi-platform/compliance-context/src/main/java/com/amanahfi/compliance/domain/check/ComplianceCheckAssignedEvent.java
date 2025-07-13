package com.amanahfi.compliance.domain.check;

import java.time.LocalDateTime;

public class ComplianceCheckAssignedEvent {
    private final String checkId;
    private final String officerId;
    private final LocalDateTime timestamp;

    public ComplianceCheckAssignedEvent(String checkId, String officerId) {
        this.checkId = checkId;
        this.officerId = officerId;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public String getOfficerId() { return officerId; }
    public LocalDateTime getTimestamp() { return timestamp; }
}