package com.loanmanagement.loan.domain.event;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.DomainEvent;
import com.loanmanagement.shared.domain.Money;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain Event: Loan Approved
 * Published when a loan application is approved
 */
@Value
@Builder
public class LoanApprovedEvent implements DomainEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    Money approvedAmount;
    LoanTerms approvedTerms;
    List<String> conditions;
    LocalDateTime approvalDate;
    LoanOfficerId approvingOfficerId;
    LocalDate approvalExpirationDate;
    boolean termsModified;
    
    @Override
    public String getAggregateId() {
        return loanId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "LoanApprovedEvent";
    }
}