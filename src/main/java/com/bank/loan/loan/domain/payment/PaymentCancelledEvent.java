package com.bank.loanmanagement.loan.domain.payment;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class PaymentCancelledEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final String reason;
    
    public PaymentCancelledEvent(String paymentId, String loanId, String customerId, String reason) {
        super(paymentId);
        this.loanId = loanId;
        this.customerId = customerId;
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "PaymentCancelled";
    }
}