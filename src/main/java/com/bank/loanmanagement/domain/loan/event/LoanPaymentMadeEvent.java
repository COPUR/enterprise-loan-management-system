package com.bank.loanmanagement.domain.loan.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;
import com.bank.loanmanagement.sharedkernel.domain.Money;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain Event: Loan Payment Made
 * 
 * Published when a customer makes a payment toward their loan.
 * This event triggers balance updates, installment marking,
 * accounting entries, and customer notifications.
 */
public class LoanPaymentMadeEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final Money paymentAmount;
    private final LocalDate paymentDate;
    public LoanPaymentMadeEvent(
        String loanId,
        String customerId,
        Money paymentAmount,
        LocalDate paymentDate
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.paymentAmount = Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        this.paymentDate = Objects.requireNonNull(paymentDate, "Payment date cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanPaymentMade";
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Money getPaymentAmount() {
        return paymentAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanPaymentMadeEvent that = (LoanPaymentMadeEvent) o;
        return Objects.equals(loanId, that.loanId) &&
               Objects.equals(customerId, that.customerId) &&
               Objects.equals(paymentDate, that.paymentDate) &&
               Objects.equals(getOccurredOn(), that.getOccurredOn());
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, customerId, paymentDate, getOccurredOn());
    }

    @Override
    public String toString() {
        return "LoanPaymentMadeEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", paymentAmount=" + paymentAmount +
                ", paymentDate=" + paymentDate +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}