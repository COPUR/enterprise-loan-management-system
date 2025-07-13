package com.bank.loan.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event published when an underwriter is assigned to a loan application
 */
@Getter
@AllArgsConstructor
@ToString
public class UnderwriterAssignedEvent implements DomainEvent {
    
    private final String eventId;
    private final String applicationId;
    private final String underwriterId;
    private final String assignedBy;
    private final String reason;
    private final LocalDateTime occurredAt;
    
    public UnderwriterAssignedEvent(String applicationId, String underwriterId, String assignedBy, 
                                  String reason, LocalDateTime occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.applicationId = applicationId;
        this.underwriterId = underwriterId;
        this.assignedBy = assignedBy;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }
    
    @Override
    public String getEventType() {
        return "UnderwriterAssigned";
    }
    
    @Override
    public String getAggregateId() {
        return applicationId;
    }
}