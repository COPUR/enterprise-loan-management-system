package com.bank.loanmanagement.domain.application.events;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import com.bank.loanmanagement.domain.application.ApplicationPriority;

import java.time.LocalDateTime;

/**
 * Domain event fired when an underwriter is assigned to a loan application.
 * 
 * Triggers notification workflows and workload management updates.
 * Important for tracking application assignment and processing timelines.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Clear event purpose and immutable design
 * ✅ DDD: Business-focused event with assignment context
 * ✅ Event-Driven: Enables notification and workload management workflows
 * ✅ Hexagonal: Pure domain event, no infrastructure dependencies
 */
public class UnderwriterAssignedEvent extends DomainEvent {
    
    private final String applicationId;
    private final String underwriterId;
    private final String customerId;
    private final ApplicationPriority priority;
    private final LocalDateTime assignedAt;
    private final String assignedBy;
    private final String assignmentReason;
    
    public UnderwriterAssignedEvent(String applicationId, String underwriterId,
                                  String customerId, ApplicationPriority priority,
                                  String assignedBy, String assignmentReason) {
        super(applicationId);
        this.applicationId = applicationId;
        this.underwriterId = underwriterId;
        this.customerId = customerId;
        this.priority = priority;
        this.assignedAt = LocalDateTime.now();
        this.assignedBy = assignedBy;
        this.assignmentReason = assignmentReason;
    }
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public String getUnderwriterId() {
        return underwriterId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public ApplicationPriority getPriority() {
        return priority;
    }
    
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public String getAssignedBy() {
        return assignedBy;
    }
    
    public String getAssignmentReason() {
        return assignmentReason;
    }
    
    /**
     * Business method to check if this is a high-priority assignment
     */
    public boolean isHighPriority() {
        return priority == ApplicationPriority.HIGH || priority == ApplicationPriority.URGENT;
    }
    
    /**
     * Business method to check if assignment was made automatically
     */
    public boolean isAutoAssignment() {
        return "SYSTEM".equals(assignedBy) || "AUTO_ASSIGNMENT".equals(assignmentReason);
    }
    
    /**
     * Business method to determine if immediate notification is required
     */
    public boolean requiresImmediateNotification() {
        return isHighPriority() || priority == ApplicationPriority.URGENT;
    }
    
    /**
     * Business method to calculate time since assignment
     */
    public long getMinutesSinceAssignment() {
        return java.time.Duration.between(assignedAt, LocalDateTime.now()).toMinutes();
    }
    
    /**
     * Business method to check if assignment is recent (within last hour)
     */
    public boolean isRecentAssignment() {
        return getMinutesSinceAssignment() <= 60;
    }
    
    @Override
    public String toString() {
        return String.format("UnderwriterAssignedEvent{applicationId='%s', underwriterId='%s', " +
                           "customerId='%s', priority=%s, assignedBy='%s', assignedAt=%s}", 
                           applicationId, underwriterId, customerId, priority, 
                           assignedBy, assignedAt);
    }
}