package com.bank.loanmanagement.loan.domain.application.events;

import com.bank.loanmanagement.loan.domain.shared.DomainEvent;
import com.bank.loanmanagement.loan.sharedkernel.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain event fired when a loan application is approved.
 * 
 * Triggers loan creation, customer notification, and underwriter workload updates.
 * Critical event for loan origination workflow continuation.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Immutable event with intention-revealing naming
 * ✅ DDD: Rich domain event with approval context
 * ✅ Event-Driven: Enables loan creation and notification workflows
 * ✅ Hexagonal: Pure domain event, no infrastructure dependencies
 */
public class LoanApplicationApprovedEvent extends DomainEvent {
    
    private final String applicationId;
    private final String customerId;
    private final String underwriterId;
    private final Money approvedAmount;
    private final BigDecimal approvedRate;
    private final String approvalReason;
    private final LocalDate approvalDate;
    private final String approverId;
    
    public LoanApplicationApprovedEvent(String applicationId, String customerId,
                                      String underwriterId, Money approvedAmount, 
                                      BigDecimal approvedRate, String approvalReason,
                                      LocalDate approvalDate, String approverId) {
        super(applicationId);
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.underwriterId = underwriterId;
        this.approvedAmount = approvedAmount;
        this.approvedRate = approvedRate;
        this.approvalReason = approvalReason;
        this.approvalDate = approvalDate;
        this.approverId = approverId;
    }
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public String getUnderwriterId() {
        return underwriterId;
    }
    
    public Money getApprovedAmount() {
        return approvedAmount;
    }
    
    public BigDecimal getApprovedRate() {
        return approvedRate;
    }
    
    public String getApprovalReason() {
        return approvalReason;
    }
    
    public LocalDate getApprovalDate() {
        return approvalDate;
    }
    
    public String getApproverId() {
        return approverId;
    }
    
    /**
     * Business method to check if this is a high-value approval
     */
    public boolean isHighValueApproval() {
        return approvedAmount.isGreaterThan(Money.of(java.math.BigDecimal.valueOf(500000), approvedAmount.getCurrency()));
    }
    
    /**
     * Business method to check if this is a premium rate approval
     */
    public boolean isPremiumRate() {
        return approvedRate.compareTo(BigDecimal.valueOf(0.05)) <= 0; // 5% or less
    }
    
    /**
     * Business method to determine if executive notification is required
     */
    public boolean requiresExecutiveNotification() {
        return isHighValueApproval() || isPremiumRate();
    }
    
    /**
     * Business method to calculate approval processing time (assuming submitted today for demo)
     */
    public long getProcessingDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(approvalDate.minusDays(7), approvalDate);
    }
    
    @Override
    public String toString() {
        return String.format("LoanApplicationApprovedEvent{applicationId='%s', customerId='%s', " +
                           "underwriterId='%s', approvedAmount=%s, approvedRate=%s, approverId='%s'}", 
                           applicationId, customerId, underwriterId, approvedAmount, 
                           approvedRate, approverId);
    }
}