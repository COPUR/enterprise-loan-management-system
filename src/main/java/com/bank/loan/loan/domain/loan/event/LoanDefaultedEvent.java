package com.bank.loanmanagement.loan.domain.loan.event;

import com.bank.loanmanagement.loan.sharedkernel.domain.DomainEvent;

import java.util.Objects;

/**
 * Domain Event: Loan Defaulted
 * 
 * Published when a loan is marked as defaulted due to non-payment.
 * This event triggers collection processes, credit bureau reporting,
 * legal actions, and risk management procedures.
 */
public class LoanDefaultedEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    public LoanDefaultedEvent(
        String loanId,
        String customerId
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanDefaulted";
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanDefaultedEvent that = (LoanDefaultedEvent) o;
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
        return "LoanDefaultedEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}