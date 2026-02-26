package com.loanmanagement.loan.domain.event;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.DomainEvent;
import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Domain Event: Loan Paid Off
 * Published when a loan is fully paid off
 */
@Value
@Builder
public class LoanPaidOffEvent implements DomainEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    Money finalPaymentAmount;
    Money totalAmountPaid;
    LocalDateTime paidOffDate;
    Money originalAmount;
    Money totalInterestPaid;
    
    @Override
    public String getAggregateId() {
        return loanId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "LoanPaidOffEvent";
    }
}