package com.bank.loanmanagement.domain.application.events;

import com.bank.loanmanagement.domain.shared.DomainEvent;
import com.bank.loanmanagement.domain.application.LoanType;
import com.bank.loanmanagement.sharedkernel.domain.Money;

import java.time.LocalDate;

/**
 * Domain event fired when a loan application is submitted.
 * 
 * Implements Event-Driven Communication for loan application workflow.
 * Triggers downstream processes like risk assessment and notifications.
 * 
 * Architecture Compliance:
 * ✅ Clean Code: Immutable event with clear naming
 * ✅ DDD: Rich domain event with business context
 * ✅ Event-Driven: Enables async processing and decoupling
 * ✅ Hexagonal: Pure domain event, no infrastructure dependencies
 */
public class LoanApplicationSubmittedEvent extends DomainEvent {
    
    private final String applicationId;
    private final String customerId;
    private final LoanType loanType;
    private final Money requestedAmount;
    private final Integer requestedTermMonths;
    private final String purpose;
    private final LocalDate applicationDate;
    private final String submittedBy;
    
    public LoanApplicationSubmittedEvent(String applicationId, String customerId, 
                                       LoanType loanType, Money requestedAmount,
                                       Integer requestedTermMonths, String purpose,
                                       LocalDate applicationDate, String submittedBy) {
        super(applicationId);
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.loanType = loanType;
        this.requestedAmount = requestedAmount;
        this.requestedTermMonths = requestedTermMonths;
        this.purpose = purpose;
        this.applicationDate = applicationDate;
        this.submittedBy = submittedBy;
    }
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public LoanType getLoanType() {
        return loanType;
    }
    
    public Money getRequestedAmount() {
        return requestedAmount;
    }
    
    public Integer getRequestedTermMonths() {
        return requestedTermMonths;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public LocalDate getApplicationDate() {
        return applicationDate;
    }
    
    public String getSubmittedBy() {
        return submittedBy;
    }
    
    /**
     * Business method to check if this is a high-value application
     */
    public boolean isHighValue() {
        return requestedAmount.isGreaterThan(Money.of(java.math.BigDecimal.valueOf(100000), requestedAmount.getCurrency()));
    }
    
    /**
     * Business method to check if this is a long-term loan
     */
    public boolean isLongTerm() {
        return requestedTermMonths != null && requestedTermMonths > 60;
    }
    
    /**
     * Business method to determine if immediate processing is required
     */
    public boolean requiresImmediateProcessing() {
        return isHighValue() || loanType == LoanType.MORTGAGE;
    }
    
    @Override
    public String toString() {
        return String.format("LoanApplicationSubmittedEvent{applicationId='%s', customerId='%s', " +
                           "loanType=%s, requestedAmount=%s, submittedBy='%s'}", 
                           applicationId, customerId, loanType, requestedAmount, submittedBy);
    }
}