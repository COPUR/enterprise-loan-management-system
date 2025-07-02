package com.bank.loanmanagement.loan.domain.customer;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;

public class CustomerActivatedEvent extends DomainEvent {
    
    public CustomerActivatedEvent(String customerId) {
        super(customerId);
    }
    
    @Override
    public String getEventType() {
        return "CustomerActivated";
    }
}