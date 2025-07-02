package com.loanmanagement.domain.event;

import java.math.BigDecimal;

public class CreditReserved extends DomainEvent {
    private final Long customerId;
    private final BigDecimal amount;
    private final BigDecimal availableCreditAfter;
    
    public CreditReserved(Long customerId, BigDecimal amount, BigDecimal availableCreditAfter) {
        super(customerId.toString());
        this.customerId = customerId;
        this.amount = amount;
        this.availableCreditAfter = availableCreditAfter;
    }
    
    // Getters
    public Long getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getAvailableCreditAfter() { return availableCreditAfter; }
}
