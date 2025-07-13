package com.amanahfi.compliance.domain.check;

import java.time.LocalDateTime;

public class ComplianceCheckCompletedEvent {
    private final String checkId;
    private final CheckStatus finalStatus;
    private final RiskScore riskScore;
    private final LocalDateTime timestamp;

    public ComplianceCheckCompletedEvent(String checkId, CheckStatus finalStatus, RiskScore riskScore) {
        this.checkId = checkId;
        this.finalStatus = finalStatus;
        this.riskScore = riskScore;
        this.timestamp = LocalDateTime.now();
    }

    public String getCheckId() { return checkId; }
    public CheckStatus getFinalStatus() { return finalStatus; }
    public RiskScore getRiskScore() { return riskScore; }
    public LocalDateTime getTimestamp() { return timestamp; }
}