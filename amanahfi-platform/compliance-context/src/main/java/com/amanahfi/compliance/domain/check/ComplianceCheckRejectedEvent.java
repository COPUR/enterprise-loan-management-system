package com.amanahfi.compliance.domain.check;

import java.time.LocalDateTime;

public class ComplianceCheckRejectedEvent {
    private final String checkId;
    private final String rejectionReason;
    private final LocalDateTime timestamp;

    public ComplianceCheckRejectedEvent(String checkId, String rejectionReason) {
        this.checkId = checkId;
        this.rejectionReason = rejectionReason;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getTimestamp() { return timestamp; }
}