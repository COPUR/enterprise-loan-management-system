package com.bank.loanmanagement.domain.loan.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;

import java.util.Objects;

/**
 * Domain Event: Loan Paid Off
 * 
 * Published when a loan is fully paid off by the customer.
 * This event triggers loan closure processes, certificate generation,
 * customer congratulations, and account status updates.
 */
public class LoanPaidOffEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    public LoanPaidOffEvent(
        String loanId,
        String customerId
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanPaidOff";
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
        LoanPaidOffEvent that = (LoanPaidOffEvent) o;
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
        return "LoanPaidOffEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}