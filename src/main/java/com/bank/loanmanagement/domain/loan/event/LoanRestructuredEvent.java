package com.bank.loanmanagement.domain.loan.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Domain Event: Loan Restructured
 * 
 * Published when a loan's terms are restructured (modified interest rate, term, etc.).
 * This event triggers new installment schedules, customer notifications,
 * accounting adjustments, and regulatory reporting.
 */
public class LoanRestructuredEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final BigDecimal newInterestRate;
    private final Integer newTermInMonths;
    private final String restructureReason;
    public LoanRestructuredEvent(
        String loanId,
        String customerId,
        BigDecimal newInterestRate,
        Integer newTermInMonths,
        String restructureReason
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.newInterestRate = Objects.requireNonNull(newInterestRate, "New interest rate cannot be null");
        this.newTermInMonths = Objects.requireNonNull(newTermInMonths, "New term cannot be null");
        this.restructureReason = Objects.requireNonNull(restructureReason, "Restructure reason cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanRestructured";
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getNewInterestRate() {
        return newInterestRate;
    }

    public Integer getNewTermInMonths() {
        return newTermInMonths;
    }

    public String getRestructureReason() {
        return restructureReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanRestructuredEvent that = (LoanRestructuredEvent) o;
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
        return "LoanRestructuredEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", newInterestRate=" + newInterestRate +
                ", newTermInMonths=" + newTermInMonths +
                ", restructureReason='" + restructureReason + '\'' +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}