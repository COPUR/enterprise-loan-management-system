package com.bank.loan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event published when a loan application is submitted
 */
@Getter
@AllArgsConstructor
@ToString
public class LoanApplicationSubmittedEvent implements DomainEvent {
    
    private final String eventId;
    private final String applicationId;
    private final String customerId;
    private final LoanType loanType;
    private final BigDecimal requestedAmount;
    private final LocalDateTime occurredAt;
    
    public LoanApplicationSubmittedEvent(String applicationId, String customerId, LoanType loanType, 
                                       BigDecimal requestedAmount, LocalDateTime occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.loanType = loanType;
        this.requestedAmount = requestedAmount;
        this.occurredAt = occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "LoanApplicationSubmitted";
    }
    
    @Override
    public String getAggregateId() {
        return applicationId;
    }
}