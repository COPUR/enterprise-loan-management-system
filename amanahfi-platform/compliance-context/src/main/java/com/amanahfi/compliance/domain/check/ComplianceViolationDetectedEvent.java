package com.amanahfi.compliance.domain.check;

import java.time.LocalDateTime;

public class ComplianceViolationDetectedEvent {
    private final String checkId;
    private final ViolationType violationType;
    private final LocalDateTime timestamp;

    public ComplianceViolationDetectedEvent(String checkId, ViolationType violationType) {
        this.checkId = checkId;
        this.violationType = violationType;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public ViolationType getViolationType() { return violationType; }
    public LocalDateTime getTimestamp() { return timestamp; }
}