package com.bank.loanmanagement.domain.customer;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class CustomerSuspendedEvent extends DomainEvent {
    
    private final String reason;
    
    public CustomerSuspendedEvent(String customerId, String reason) {
        super(customerId);
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "CustomerSuspended";
    }
}