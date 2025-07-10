package com.loanmanagement.loan.domain.event;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.DomainEvent;
import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Domain Event: Loan Disbursed
 * Published when loan funds are disbursed to the customer
 */
@Value
@Builder
public class LoanDisbursedEvent implements DomainEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    Money disbursedAmount;
    DisbursementMethod disbursementMethod;
    String accountNumber;
    String routingNumber;
    LocalDateTime disbursementDate;
    LoanOfficerId disbursedBy;
    String specialInstructions;
    
    @Override
    public String getAggregateId() {
        return loanId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "LoanDisbursedEvent";
    }
}