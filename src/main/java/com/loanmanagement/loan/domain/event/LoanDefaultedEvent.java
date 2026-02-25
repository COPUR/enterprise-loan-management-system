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
 * Domain Event: Loan Defaulted
 * Published when a loan is marked as defaulted
 */
@Value
@Builder
public class LoanDefaultedEvent implements DomainEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    String defaultReason;
    int daysPastDue;
    Money totalAmountPastDue;
    int missedPayments;
    LocalDate lastPaymentDate;
    List<String> collectionActions;
    LocalDateTime defaultDate;
    LoanOfficerId officerId;
    
    @Override
    public String getAggregateId() {
        return loanId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "LoanDefaultedEvent";
    }
}