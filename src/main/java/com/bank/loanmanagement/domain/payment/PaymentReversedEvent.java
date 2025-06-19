package com.bank.loanmanagement.domain.payment;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import com.bank.loanmanagement.domain.shared.Money;
import lombok.Getter;

@Getter
public class PaymentReversedEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final Money amount;
    private final String reason;
    
    public PaymentReversedEvent(String paymentId, String loanId, String customerId, Money amount, String reason) {
        super(paymentId);
        this.loanId = loanId;
        this.customerId = customerId;
        this.amount = amount;
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "PaymentReversed";
    }
}