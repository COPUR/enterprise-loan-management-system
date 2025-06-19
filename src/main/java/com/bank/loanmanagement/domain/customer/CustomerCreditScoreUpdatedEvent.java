package com.bank.loanmanagement.domain.customer;

import com.bank.loanmanagement.domain.shared.DomainEvent;
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