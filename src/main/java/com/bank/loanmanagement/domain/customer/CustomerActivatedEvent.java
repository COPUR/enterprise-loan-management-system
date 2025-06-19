package com.bank.loanmanagement.domain.customer;

import com.bank.loanmanagement.domain.shared.DomainEvent;

public class CustomerActivatedEvent extends DomainEvent {
    
    public CustomerActivatedEvent(String customerId) {
        super(customerId);
    }
    
    @Override
    public String getEventType() {
        return "CustomerActivated";
    }
}