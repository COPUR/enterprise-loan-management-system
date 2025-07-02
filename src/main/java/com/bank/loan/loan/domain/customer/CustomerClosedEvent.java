package com.bank.loanmanagement.loan.domain.customer;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
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