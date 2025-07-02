package com.bank.loanmanagement.loan.domain.loan.event;

import com.bank.loanmanagement.loan.sharedkernel.domain.DomainEvent;
import com.bank.loanmanagement.loan.sharedkernel.domain.Money;

import java.util.Objects;

/**
 * Domain Event: Loan Disbursed
 * 
 * Published when loan funds are disbursed to the customer's account.
 * This event triggers fund transfer, accounting entries, customer notifications,
 * and activation of repayment schedules.
 */
public class LoanDisbursedEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final Money disbursedAmount;
    public LoanDisbursedEvent(
        String loanId,
        String customerId,
        Money disbursedAmount
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.disbursedAmount = Objects.requireNonNull(disbursedAmount, "Disbursed amount cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanDisbursed";
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Money getDisbursedAmount() {
        return disbursedAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanDisbursedEvent that = (LoanDisbursedEvent) o;
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
        return "LoanDisbursedEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", disbursedAmount=" + disbursedAmount +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}