package com.bank.loanmanagement.loan.domain.loan;

import com.bank.loanmanagement.loan.domain.shared.AggregateRoot;
import com.bank.loanmanagement.loan.domain.shared.Money;

import java.math.BigDecimal;

public class LoanAggregate extends AggregateRoot<LoanId> {

    private final Money amount;
    private final InterestRate interestRate;
    private final LoanTerm term;
    private LoanStatus status;

    public LoanAggregate(LoanId loanId, Money amount, InterestRate interestRate, LoanTerm term) {
        super(loanId);
        this.amount = amount;
        this.interestRate = interestRate;
        this.term = term;
        this.status = LoanStatus.PENDING;
    }

    public Money getAmount() {
        return amount;
    }

    public InterestRate getInterestRate() {
        return interestRate;
    }

    public LoanTerm getTerm() {
        return term;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void approve() {
        this.status = LoanStatus.APPROVED;
    }

    public void reject() {
        this.status = LoanStatus.REJECTED;
    }
}
