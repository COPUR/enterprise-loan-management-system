package com.amanahfi.murabaha.domain.contract;

import java.time.LocalDateTime;

public class ContractActivatedEvent {
    private final String contractId;
    private final String activationReference;
    private final LocalDateTime timestamp;

    public ContractActivatedEvent(String contractId, String activationReference) {
        this.contractId = contractId;
        this.activationReference = activationReference;
        this.timestamp = LocalDateTime.now();
    }

    public String getContractId() { return contractId; }
    public String getActivationReference() { return activationReference; }
    public LocalDateTime getTimestamp() { return timestamp; }
}