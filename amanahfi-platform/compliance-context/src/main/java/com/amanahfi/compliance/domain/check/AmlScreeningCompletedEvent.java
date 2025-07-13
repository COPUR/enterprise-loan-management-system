package com.amanahfi.compliance.domain.check;

import java.time.LocalDateTime;

public class AmlScreeningCompletedEvent {
    private final String checkId;
    private final RiskScore riskScore;
    private final LocalDateTime timestamp;

    public AmlScreeningCompletedEvent(String checkId, RiskScore riskScore) {
        this.checkId = checkId;
        this.riskScore = riskScore;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public RiskScore getRiskScore() { return riskScore; }
    public LocalDateTime getTimestamp() { return timestamp; }
}