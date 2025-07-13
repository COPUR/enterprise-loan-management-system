package com.amanahfi.murabaha.domain.contract;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class MurabahaContractCreatedEvent {
    private final String contractId;
    private final String customerId;
    private final Money assetCost;
    private final Money profitAmount;
    private final LocalDateTime timestamp;

    public MurabahaContractCreatedEvent(String contractId, String customerId, Money assetCost, Money profitAmount) {
        this.contractId = contractId;
        this.customerId = customerId;
        this.assetCost = assetCost;
        this.profitAmount = profitAmount;
        this.timestamp = LocalDateTime.now();
    }

    public String getContractId() { return contractId; }
    public String getCustomerId() { return customerId; }
    public Money getAssetCost() { return assetCost; }
    public Money getProfitAmount() { return profitAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}