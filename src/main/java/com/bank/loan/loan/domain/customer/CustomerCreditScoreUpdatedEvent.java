package com.bank.loanmanagement.loan.domain.customer;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class CustomerCreditScoreUpdatedEvent extends DomainEvent {
    
    private final int newScore;
    
    public CustomerCreditScoreUpdatedEvent(String customerId, int newScore) {
        super(customerId);
        this.newScore = newScore;
    }
    
    @Override
    public String getEventType() {
        return "CustomerCreditScoreUpdated";
    }
}