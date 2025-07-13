package com.amanahfi.murabaha.domain.contract;

import com.amanahfi.shared.domain.money.Money;
import java.time.LocalDateTime;

public class InstallmentPaidEvent {
    private final String contractId;
    private final Integer installmentNumber;
    private final Money amount;
    private final LocalDateTime timestamp;

    public InstallmentPaidEvent(String contractId, Integer installmentNumber, Money amount) {
        this.contractId = contractId;
        this.installmentNumber = installmentNumber;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getContractId() { return contractId; }
    public Integer getInstallmentNumber() { return installmentNumber; }
    public Money getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}