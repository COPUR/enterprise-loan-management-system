package com.bank.loanmanagement.domain.loan.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;

import java.util.Objects;

/**
 * Domain Event: Loan Rejected
 * 
 * Published when a loan application is rejected by the banking authority.
 * This event triggers customer notifications, document cleanup,
 * and audit trail entries.
 */
public class LoanRejectedEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final String rejectionReason;
    public LoanRejectedEvent(
        String loanId,
        String customerId,
        String rejectionReason
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.rejectionReason = Objects.requireNonNull(rejectionReason, "Rejection reason cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanRejected";
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanRejectedEvent that = (LoanRejectedEvent) o;
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
        return "LoanRejectedEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", rejectionReason='" + rejectionReason + '\'' +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}