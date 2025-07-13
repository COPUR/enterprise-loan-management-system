package com.bank.loan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event published when a loan application is approved
 */
@Getter
@AllArgsConstructor
@ToString
public class LoanApplicationApprovedEvent implements DomainEvent {
    
    private final String eventId;
    private final String applicationId;
    private final String customerId;
    private final BigDecimal approvedAmount;
    private final BigDecimal approvedRate;
    private final String approverId;
    private final LocalDateTime occurredAt;
    
    public LoanApplicationApprovedEvent(String applicationId, String customerId, BigDecimal approvedAmount,
                                      BigDecimal approvedRate, String approverId, LocalDateTime occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.approvedAmount = approvedAmount;
        this.approvedRate = approvedRate;
        this.approverId = approverId;
        this.occurredAt = occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "LoanApplicationApproved";
    }
    
    @Override
    public String getAggregateId() {
        return applicationId;
    }
}