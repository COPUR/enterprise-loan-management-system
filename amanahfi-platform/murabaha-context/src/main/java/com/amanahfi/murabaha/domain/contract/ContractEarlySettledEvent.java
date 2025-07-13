package com.amanahfi.murabaha.domain.contract;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class ContractEarlySettledEvent {
    private final String contractId;
    private final Money settlementAmount;
    private final LocalDateTime timestamp;

    public ContractEarlySettledEvent(String contractId, Money settlementAmount) {
        this.contractId = contractId;
        this.settlementAmount = settlementAmount;
        this.timestamp = LocalDateTime.now();
    }

    public String getContractId() { return contractId; }
    public Money getSettlementAmount() { return settlementAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}