package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import com.bank.loanmanagement.domain.shared.Money;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class LoanPaymentMadeEvent extends DomainEvent {
    
    private final String customerId;
    private final Money paymentAmount;
    private final LocalDate paymentDate;
    
    public LoanPaymentMadeEvent(String loanId, String customerId, Money paymentAmount, LocalDate paymentDate) {
        super(loanId);
        this.customerId = customerId;
        this.paymentAmount = paymentAmount;
        this.paymentDate = paymentDate;
    }
    
    @Override
    public String getEventType() {
        return "LoanPaymentMade";
    }
}