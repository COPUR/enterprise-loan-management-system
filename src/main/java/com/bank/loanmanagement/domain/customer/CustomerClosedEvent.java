package com.bank.loanmanagement.domain.customer;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class CustomerClosedEvent extends DomainEvent {
    
    private final String reason;
    
    public CustomerClosedEvent(String customerId, String reason) {
        super(customerId);
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "CustomerClosed";
    }
}