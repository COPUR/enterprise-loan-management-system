package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import com.bank.loanmanagement.domain.shared.Money;
import lombok.Getter;

@Getter
public class LoanDisbursedEvent extends DomainEvent {
    
    private final String customerId;
    private final Money amount;
    
    public LoanDisbursedEvent(String loanId, String customerId, Money amount) {
        super(loanId);
        this.customerId = customerId;
        this.amount = amount;
    }
    
    @Override
    public String getEventType() {
        return "LoanDisbursed";
    }
}