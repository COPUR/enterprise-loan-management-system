package com.amanahfi.murabaha.domain.contract;

import java.time.LocalDateTime;

public class AssetDeliveredEvent {
    private final String contractId;
    private final String deliveryReference;
    private final LocalDateTime timestamp;

    public AssetDeliveredEvent(String contractId, String deliveryReference) {
        this.contractId = contractId;
        this.deliveryReference = deliveryReference;
        this.timestamp = LocalDateTime.now();
    }

    public String getContractId() { return contractId; }
    public String getDeliveryReference() { return deliveryReference; }
    public LocalDateTime getTimestamp() { return timestamp; }
}