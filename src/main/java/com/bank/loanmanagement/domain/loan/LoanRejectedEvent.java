package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class LoanRejectedEvent extends DomainEvent {
    
    private final String customerId;
    private final String reason;
    
    public LoanRejectedEvent(String loanId, String customerId, String reason) {
        super(loanId);
        this.customerId = customerId;
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "LoanRejected";
    }
}