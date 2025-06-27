package com.banking.loan.domain.events;

import com.banking.loan.domain.shared.BaseDomainEvent;
import com.banking.loan.domain.shared.EventMetadata;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Domain event fired when a loan is approved
 */
public class LoanApprovedEvent extends BaseDomainEvent {
    
    private final String customerId;
    private final BigDecimal amount;
    private final BigDecimal interestRate;
    private final LocalDate firstPaymentDate;
    
    public LoanApprovedEvent(String loanId, Long version, String approvedBy, 
                           String correlationId, String tenantId, EventMetadata metadata,
                           String customerId, BigDecimal amount, BigDecimal interestRate,
                           LocalDate firstPaymentDate) {
        super(loanId, version, approvedBy, correlationId, tenantId, metadata);
        this.customerId = customerId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.firstPaymentDate = firstPaymentDate;
    }
    
    @Override
    public String getEventType() {
        return "LoanApproved";
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public BigDecimal getInterestRate() {
        return interestRate;
    }
    
    public LocalDate getFirstPaymentDate() {
        return firstPaymentDate;
    }
}