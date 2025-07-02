package com.bank.loanmanagement.loan.domain.payment;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
import com.bank.loanmanagement.loan.domain.shared.Money;
import lombok.Getter;

@Getter
public class PaymentProcessedEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final Money amount;
    
    public PaymentProcessedEvent(String paymentId, String loanId, String customerId, Money amount) {
        super(paymentId);
        this.loanId = loanId;
        this.customerId = customerId;
        this.amount = amount;
    }
    
    @Override
    public String getEventType() {
        return "PaymentProcessed";
    }
}