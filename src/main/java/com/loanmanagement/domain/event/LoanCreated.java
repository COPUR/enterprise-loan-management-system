// domain/event/LoanCreated.java
package com.loanmanagement.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanCreated extends DomainEvent {
    private final Long loanId;
    private final Long customerId;
    private final BigDecimal principalAmount;
    private final BigDecimal loanAmount;
    private final BigDecimal interestRate;
    private final Integer numberOfInstallments;
    private final LocalDate createDate;
    
    public LoanCreated(
        Long loanId,
        Long customerId,
        BigDecimal principalAmount,
        BigDecimal loanAmount,
        BigDecimal interestRate,
        Integer numberOfInstallments,
        LocalDate createDate
    ) {
        super(loanId.toString());
        this.loanId = loanId;
        this.customerId = customerId;
        this.principalAmount = principalAmount;
        this.loanAmount = loanAmount;
        this.interestRate = interestRate;
        this.numberOfInstallments = numberOfInstallments;
        this.createDate = createDate;
    }
    
    // Getters
    public Long getLoanId() { return loanId; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public Integer getNumberOfInstallments() { return numberOfInstallments; }
    public LocalDate getCreateDate() { return createDate; }
}

// domain/event/PaymentMade.java

// domain/event/CreditReserved.java

// domain/event/CreditReleased.java


// domain/event/PaymentFailed.java
