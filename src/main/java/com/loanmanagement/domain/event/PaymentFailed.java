package com.loanmanagement.domain.event;

import java.math.BigDecimal;

public class PaymentFailed extends DomainEvent {
    private final Long loanId;
    private final Long customerId;
    private final BigDecimal attemptedAmount;
    private final String failureReason;
    
    public PaymentFailed(Long loanId, Long customerId, BigDecimal attemptedAmount, String failureReason) {
        super(loanId.toString());
        this.loanId = loanId;
        this.customerId = customerId;
        this.attemptedAmount = attemptedAmount;
        this.failureReason = failureReason;
    }
    
    // Getters
    public Long getLoanId() { return loanId; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getAttemptedAmount() { return attemptedAmount; }
    public String getFailureReason() { return failureReason; }
}