package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.AggregateRoot;
import com.bank.shared.kernel.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Loan Aggregate Root
 * 
 * Represents a financial loan with its terms, payment schedule,
 * and business rules for loan lifecycle management.
 */
public class Loan extends AggregateRoot<LoanId> {
    
    private LoanId loanId;
    private CustomerId customerId;
    private Money principalAmount;
    private InterestRate interestRate;
    private LoanTerm loanTerm;
    private LoanStatus status;
    private LocalDate applicationDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private Money outstandingBalance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Private constructor for JPA
    protected Loan() {}
    
    private Loan(LoanId loanId, CustomerId customerId, Money principalAmount,
                InterestRate interestRate, LoanTerm loanTerm) {
        this.loanId = Objects.requireNonNull(loanId, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.principalAmount = Objects.requireNonNull(principalAmount, "Principal amount cannot be null");
        this.interestRate = Objects.requireNonNull(interestRate, "Interest rate cannot be null");
        this.loanTerm = Objects.requireNonNull(loanTerm, "Loan term cannot be null");
        this.status = LoanStatus.CREATED;
        this.applicationDate = LocalDate.now();
        this.outstandingBalance = principalAmount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        validateLoanData();
        
        // Domain event
        addDomainEvent(new LoanCreatedEvent(loanId, customerId, principalAmount));
    }
    
    public static Loan create(LoanId loanId, CustomerId customerId, Money principalAmount,
                             InterestRate interestRate, LoanTerm loanTerm) {
        return new Loan(loanId, customerId, principalAmount, interestRate, loanTerm);
    }
    
    private void validateLoanData() {
        if (principalAmount.isNegative() || principalAmount.isZero()) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }
        if (interestRate.isNegative()) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (loanTerm.getMonths() <= 0) {
            throw new IllegalArgumentException("Loan term must be positive");
        }
    }
    
    @Override
    public LoanId getId() {
        return loanId;
    }
    
    public CustomerId getCustomerId() {
        return customerId;
    }
    
    public Money getPrincipalAmount() {
        return principalAmount;
    }
    
    public InterestRate getInterestRate() {
        return interestRate;
    }
    
    public LoanTerm getLoanTerm() {
        return loanTerm;
    }
    
    public LoanStatus getStatus() {
        return status;
    }
    
    public LocalDate getApplicationDate() {
        return applicationDate;
    }
    
    public LocalDate getApprovalDate() {
        return approvalDate;
    }
    
    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }
    
    public LocalDate getMaturityDate() {
        return maturityDate;
    }
    
    public Money getOutstandingBalance() {
        return outstandingBalance;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void approve() {
        if (!status.canBeApproved()) {
            throw new IllegalStateException("Loan cannot be approved in current status: " + status);
        }
        this.status = LoanStatus.APPROVED;
        this.approvalDate = LocalDate.now();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanApprovedEvent(loanId, customerId, principalAmount));
    }
    
    public void reject(String reason) {
        if (!status.canBeRejected()) {
            throw new IllegalStateException("Loan cannot be rejected in current status: " + status);
        }
        this.status = LoanStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanRejectedEvent(loanId, customerId, reason));
    }
    
    public void disburse() {
        if (!status.canBeDisbursed()) {
            throw new IllegalStateException("Loan cannot be disbursed in current status: " + status);
        }
        this.status = LoanStatus.DISBURSED;
        this.disbursementDate = LocalDate.now();
        this.maturityDate = calculateMaturityDate();
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanDisbursedEvent(loanId, customerId, principalAmount, disbursementDate));
    }
    
    public void cancel(String reason) {
        if (!status.canBeCancelled()) {
            throw new IllegalStateException("Loan cannot be cancelled in current status: " + status);
        }
        this.status = LoanStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanCancelledEvent(loanId, customerId, reason));
    }
    
    public void makePayment(Money paymentAmount) {
        if (!status.canAcceptPayments()) {
            throw new IllegalStateException("Loan cannot accept payments in current status: " + status);
        }
        if (paymentAmount.isNegative() || paymentAmount.isZero()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentAmount.compareTo(outstandingBalance) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed outstanding balance");
        }
        
        Money previousBalance = this.outstandingBalance;
        this.outstandingBalance = this.outstandingBalance.subtract(paymentAmount);
        this.updatedAt = LocalDateTime.now();
        
        if (this.outstandingBalance.isZero()) {
            this.status = LoanStatus.FULLY_PAID;
            addDomainEvent(new LoanFullyPaidEvent(loanId, customerId));
        }
        
        addDomainEvent(new LoanPaymentMadeEvent(loanId, customerId, paymentAmount, previousBalance, outstandingBalance));
    }
    
    private LocalDate calculateMaturityDate() {
        return disbursementDate.plusMonths(loanTerm.getMonths());
    }
    
    public boolean isOverdue() {
        return LocalDate.now().isAfter(maturityDate) && !outstandingBalance.isZero();
    }
    
    public Money calculateMonthlyPayment() {
        if (loanTerm.getMonths() == 0) {
            return principalAmount;
        }
        
        BigDecimal monthlyRate = interestRate.getMonthlyRate();
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principalAmount.divide(BigDecimal.valueOf(loanTerm.getMonths()));
        }
        
        // PMT formula: P * [r(1+r)^n] / [(1+r)^n - 1]
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        BigDecimal onePlusRateToN = onePlusRate.pow(loanTerm.getMonths());
        BigDecimal numerator = monthlyRate.multiply(onePlusRateToN);
        BigDecimal denominator = onePlusRateToN.subtract(BigDecimal.ONE);
        BigDecimal paymentFactor = numerator.divide(denominator, 10, java.math.RoundingMode.HALF_UP);
        
        return principalAmount.multiply(paymentFactor);
    }
}