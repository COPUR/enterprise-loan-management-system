package com.bank.loanmanagement.domain.loan.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;
import com.bank.loanmanagement.sharedkernel.domain.Money;

import java.util.Objects;

/**
 * Domain Event: Loan Approved
 * 
 * Published when a loan application is approved by the banking authority.
 * This event triggers disbursement processes, customer notifications,
 * and accounting entries.
 */
public class LoanApprovedEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final Money approvedAmount;
    private final String approvedBy;

    public LoanApprovedEvent(
        String loanId,
        String customerId,
        Money approvedAmount,
        String approvedBy
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.approvedAmount = Objects.requireNonNull(approvedAmount, "Approved amount cannot be null");
        this.approvedBy = Objects.requireNonNull(approvedBy, "Approved by cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanApproved";
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Money getApprovedAmount() {
        return approvedAmount;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanApprovedEvent that = (LoanApprovedEvent) o;
        return Objects.equals(loanId, that.loanId) &&
               Objects.equals(customerId, that.customerId) &&
               Objects.equals(getOccurredOn(), that.getOccurredOn());
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, customerId, getOccurredOn());
    }

    @Override
    public String toString() {
        return "LoanApprovedEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", approvedAmount=" + approvedAmount +
                ", approvedBy='" + approvedBy + '\'' +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}