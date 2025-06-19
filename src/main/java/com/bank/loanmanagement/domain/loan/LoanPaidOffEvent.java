package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import lombok.Getter;

@Getter
public class LoanPaidOffEvent extends DomainEvent {
    
    private final String customerId;
    
    public LoanPaidOffEvent(String loanId, String customerId) {
        super(loanId);
        this.customerId = customerId;
    }
    
    @Override
    public String getEventType() {
        return "LoanPaidOff";
    }
}