package com.loanmanagement.loan.domain.event;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.DomainEvent;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain Event: Loan Rejected
 * Published when a loan application is rejected
 */
@Value
@Builder
public class LoanRejectedEvent implements DomainEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    String primaryReason;
    List<String> rejectionDetails;
    boolean appealable;
    LocalDate appealDeadline;
    LocalDateTime rejectionDate;
    LoanOfficerId rejectingOfficerId;
    
    @Override
    public String getAggregateId() {
        return loanId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "LoanRejectedEvent";
    }
}