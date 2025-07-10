package com.loanmanagement.loan.domain.event;

import com.loanmanagement.loan.domain.model.*;
import com.loanmanagement.shared.domain.DomainEvent;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain Event: Loan Restructured
 * Published when loan terms are restructured
 */
@Value
@Builder
public class LoanRestructuredEvent implements DomainEvent {
    
    String eventId;
    LocalDateTime occurredOn;
    LoanId loanId;
    CustomerId customerId;
    LoanTerms originalTerms;
    LoanTerms newTerms;
    String restructuringReason;
    String justification;
    boolean temporaryHardship;
    Integer expectedDuration;
    LocalDateTime restructureDate;
    LoanOfficerId officerId;
    Map<String, Object> termsComparison;
    
    @Override
    public String getAggregateId() {
        return loanId.getValue();
    }
    
    @Override
    public String getEventType() {
        return "LoanRestructuredEvent";
    }
}