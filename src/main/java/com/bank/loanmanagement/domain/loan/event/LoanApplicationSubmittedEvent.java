package com.bank.loanmanagement.domain.loan.event;

import com.bank.loanmanagement.sharedkernel.domain.DomainEvent;
import com.bank.loanmanagement.sharedkernel.domain.Money;
import com.bank.loanmanagement.domain.loan.LoanType;

import java.util.Objects;

/**
 * Domain Event: Loan Application Submitted
 * 
 * Published when a customer submits a new loan application.
 * This event triggers downstream processes like credit checking,
 * document verification, and approval workflows.
 */
public class LoanApplicationSubmittedEvent extends DomainEvent {
    
    private final String loanId;
    private final String customerId;
    private final Money requestedAmount;
    private final LoanType loanType;
    private final String purpose;

    public LoanApplicationSubmittedEvent(
        String loanId,
        String customerId,
        Money requestedAmount,
        LoanType loanType,
        String purpose
    ) {
        super();
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.requestedAmount = Objects.requireNonNull(requestedAmount, "Requested amount cannot be null");
        this.loanType = Objects.requireNonNull(loanType, "Loan type cannot be null");
        this.purpose = Objects.requireNonNull(purpose, "Purpose cannot be null");
    }

    @Override
    public String getEventType() {
        return "LoanApplicationSubmitted";
    }

    public String getLoanId() {
        return loanId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Money getRequestedAmount() {
        return requestedAmount;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public String getPurpose() {
        return purpose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanApplicationSubmittedEvent that = (LoanApplicationSubmittedEvent) o;
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
        return "LoanApplicationSubmittedEvent{" +
                "loanId='" + loanId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", requestedAmount=" + requestedAmount +
                ", loanType=" + loanType +
                ", purpose='" + purpose + '\'' +
                ", occurredOn=" + getOccurredOn() +
                '}';
    }
}