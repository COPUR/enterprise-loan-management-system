
package com.bank.loanmanagement.domain.model;

import com.bank.loanmanagement.sharedkernel.domain.AggregateRoot;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loan aggregate root representing a loan contract.
 * Encapsulates loan business rules and installment generation logic.
 */
public class Loan extends AggregateRoot {
    
    private static final BigDecimal MIN_LOAN_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("500000.00");
    private static final BigDecimal MIN_INTEREST_RATE = new BigDecimal("0.05");
    private static final BigDecimal MAX_INTEREST_RATE = new BigDecimal("0.30");
    private static final int MIN_INSTALLMENTS = 6;
    private static final int MAX_INSTALLMENTS = 60;
    
    private Long loanId;
    private Long customerId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer numberOfInstallments;
    private LocalDate createDate;
    private List<LoanInstallment> installments;
    private LoanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Default constructor for JPA
    protected Loan() {
        this.installments = new ArrayList<>();
        this.status = LoanStatus.PENDING;
        this.createDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }
    
    // Business constructor
    public Loan(Long customerId, BigDecimal loanAmount, BigDecimal interestRate, Integer numberOfInstallments) {
        this();
        this.customerId = validateCustomerId(customerId);
        this.loanAmount = validateLoanAmount(loanAmount);
        this.interestRate = validateInterestRate(interestRate);
        this.numberOfInstallments = validateNumberOfInstallments(numberOfInstallments);
        generateInstallments();
    }
    
    // Business methods
    public void approve() {
        if (this.status != LoanStatus.PENDING) {
            throw new IllegalLoanStateException("Can only approve pending loans");
        }
        this.status = LoanStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void reject(String reason) {
        if (this.status != LoanStatus.PENDING) {
            throw new IllegalLoanStateException("Can only reject pending loans");
        }
        this.status = LoanStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void disburse() {
        if (this.status != LoanStatus.APPROVED) {
            throw new IllegalLoanStateException("Can only disburse approved loans");
        }
        this.status = LoanStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void payOff() {
        if (this.status != LoanStatus.ACTIVE) {
            throw new IllegalLoanStateException("Can only pay off active loans");
        }
        this.status = LoanStatus.PAID_OFF;
        this.updatedAt = LocalDateTime.now();
    }
    
    public BigDecimal getTotalAmount() {
        return installments.stream()
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public BigDecimal getTotalInterest() {
        return getTotalAmount().subtract(loanAmount);
    }
    
    public BigDecimal getRemainingAmount() {
        return installments.stream()
                .filter(installment -> !installment.isPaid())
                .map(LoanInstallment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public int getRemainingInstallments() {
        return (int) installments.stream()
                .filter(installment -> !installment.isPaid())
                .count();
    }
    
    public boolean isFullyPaid() {
        return installments.stream().allMatch(LoanInstallment::isPaid);
    }
    
    public List<LoanInstallment> getOverdueInstallments() {
        LocalDate today = LocalDate.now();
        return installments.stream()
                .filter(installment -> !installment.isPaid() && installment.getDueDate().isBefore(today))
                .toList();
    }
    
    // Private business logic methods
    private void generateInstallments() {
        BigDecimal monthlyPayment = calculateMonthlyPayment();
        LocalDate currentDueDate = createDate.plusMonths(1);
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            LoanInstallment installment = new LoanInstallment(
                    this.loanId, i, monthlyPayment, currentDueDate);
            installments.add(installment);
            currentDueDate = currentDueDate.plusMonths(1);
        }
    }
    
    private BigDecimal calculateMonthlyPayment() {
        // Calculate using the standard loan payment formula
        // M = P * [r(1+r)^n] / [(1+r)^n - 1]
        // Where: M = monthly payment, P = principal, r = monthly interest rate, n = number of payments
        
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
        
        // Calculate (1+r)^n
        BigDecimal onePlusRatePowerN = onePlusRate.pow(numberOfInstallments);
        
        // Calculate numerator: r(1+r)^n
        BigDecimal numerator = monthlyRate.multiply(onePlusRatePowerN);
        
        // Calculate denominator: (1+r)^n - 1
        BigDecimal denominator = onePlusRatePowerN.subtract(BigDecimal.ONE);
        
        // Calculate monthly payment
        return loanAmount.multiply(numerator.divide(denominator, 2, RoundingMode.HALF_UP));
    }
    
    // Validation methods
    private Long validateCustomerId(Long customerId) {
        if (customerId == null || customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be a positive number");
        }
        return customerId;
    }
    
    private BigDecimal validateLoanAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Loan amount cannot be null");
        }
        if (amount.compareTo(MIN_LOAN_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                    String.format("Loan amount must be at least %s", MIN_LOAN_AMOUNT));
        }
        if (amount.compareTo(MAX_LOAN_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    String.format("Loan amount cannot exceed %s", MAX_LOAN_AMOUNT));
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal validateInterestRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Interest rate cannot be null");
        }
        if (rate.compareTo(MIN_INTEREST_RATE) < 0) {
            throw new IllegalArgumentException(
                    String.format("Interest rate must be at least %s", MIN_INTEREST_RATE));
        }
        if (rate.compareTo(MAX_INTEREST_RATE) > 0) {
            throw new IllegalArgumentException(
                    String.format("Interest rate cannot exceed %s", MAX_INTEREST_RATE));
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }
    
    private Integer validateNumberOfInstallments(Integer installments) {
        if (installments == null) {
            throw new IllegalArgumentException("Number of installments cannot be null");
        }
        if (installments < MIN_INSTALLMENTS) {
            throw new IllegalArgumentException(
                    String.format("Number of installments must be at least %d", MIN_INSTALLMENTS));
        }
        if (installments > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException(
                    String.format("Number of installments cannot exceed %d", MAX_INSTALLMENTS));
        }
        return installments;
    }
    
    // Getters
    public Long getLoanId() { return loanId; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getLoanAmount() { return loanAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public Integer getNumberOfInstallments() { return numberOfInstallments; }
    public LocalDate getCreateDate() { return createDate; }
    public List<LoanInstallment> getInstallments() { return new ArrayList<>(installments); }
    public LoanStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    
    // Package-private setters for JPA
    void setLoanId(Long loanId) { this.loanId = loanId; }
    void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    void setInstallments(List<LoanInstallment> installments) { this.installments = installments; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Loan)) return false;
        Loan loan = (Loan) o;
        return Objects.equals(loanId, loan.loanId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }
    
    @Override
    public String toString() {
        return String.format("Loan{id=%d, customerId=%d, amount=%s, status=%s}", 
                           loanId, customerId, loanAmount, status);
    }
}
