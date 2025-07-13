package com.amanahfi.murabaha.domain.contract;

import java.time.LocalDateTime;

public class ContractDefaultedEvent {
    private final String contractId;
    private final String reason;
    private final LocalDateTime timestamp;

    public ContractDefaultedEvent(String contractId, String reason) {
        this.contractId = contractId;
        this.reason = reason;
        this.timestamp = LocalDateTime.now();
    }

    public String getContractId() { return contractId; }
    public String getReason() { return reason; }
    public LocalDateTime getTimestamp() { return timestamp; }
}