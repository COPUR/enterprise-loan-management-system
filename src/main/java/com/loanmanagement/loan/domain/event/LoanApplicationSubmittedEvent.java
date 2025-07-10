package com.loanmanagement.loan.domain.event;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.DomainEvent;
import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Domain Event: Loan Application Submitted
 * Published when a new loan application is submitted
 */
@Value
@Builder
public class LoanApplicationSubmittedEvent implements DomainEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    Money requestedAmount;
    LoanPurpose loanPurpose;
    LoanTerms requestedTerms;
    LoanOfficerId loanOfficerId;
    LocalDateTime applicationDate;
    
    @Override
    public String getAggregateId() {
        return loanId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "LoanApplicationSubmittedEvent";
    }
}