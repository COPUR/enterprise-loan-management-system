package com.bank.loanmanagement.loan.domain.loan;

import com.bank.loan.loan.domain.customer.CustomerId;
import com.bank.loanmanagement.loan.sharedkernel.domain.AggregateRoot;
import com.bank.loanmanagement.loan.sharedkernel.domain.Money;
import com.bank.loanmanagement.loan.domain.loan.event.LoanApplicationSubmittedEvent;
import com.bank.loanmanagement.loan.domain.loan.event.LoanApprovedEvent;
import com.bank.loanmanagement.loan.domain.loan.event.LoanRejectedEvent;
import com.bank.loanmanagement.loan.domain.loan.event.LoanDisbursedEvent;
import com.bank.loanmanagement.loan.domain.loan.event.LoanPaymentMadeEvent;
import com.bank.loanmanagement.loan.domain.loan.event.LoanPaidOffEvent;
import com.bank.loanmanagement.loan.domain.loan.event.LoanDefaultedEvent;
import com.bank.loanmanagement.loan.domain.loan.event.LoanRestructuredEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loan Domain Aggregate Root - Clean DDD Implementation
 * 
 * Represents a banking loan with complete business logic for:
 * - Loan application and approval workflow
 * - Payment processing and installment management
 * - Interest calculations and amortization
 * - Default management and compliance
 * 
 * Pure domain model without infrastructure dependencies.
 */
public class Loan extends AggregateRoot<LoanId> {
    
    private LoanId id;
    private CustomerId customerId;
    private Money principalAmount;
    private Money outstandingBalance;
    private BigDecimal interestRate;
    private Integer termInMonths;
    private LoanType loanType;
    private LoanStatus status;
    private LocalDate applicationDate;
    private LocalDate approvalDate;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private List<LoanInstallment> installments;
    private LoanTerms loanTerms;
    private String purpose;
    private String collateralDescription;
    private String approvedBy;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    // Private constructor for domain creation
    private Loan(
        LoanId id,
        CustomerId customerId,
        Money principalAmount,
        BigDecimal interestRate,
        Integer termInMonths,
        LoanType loanType,
        String purpose
    ) {
        this.id = Objects.requireNonNull(id, "Loan ID cannot be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID cannot be null");
        this.principalAmount = Objects.requireNonNull(principalAmount, "Principal amount cannot be null");
        this.interestRate = Objects.requireNonNull(interestRate, "Interest rate cannot be null");
        this.termInMonths = Objects.requireNonNull(termInMonths, "Term in months cannot be null");
        this.loanType = Objects.requireNonNull(loanType, "Loan type cannot be null");
        this.purpose = Objects.requireNonNull(purpose, "Purpose cannot be null");
        
        // Initialize defaults
        this.status = LoanStatus.PENDING;
        this.applicationDate = LocalDate.now();
        this.installments = new ArrayList<>();
        this.outstandingBalance = Money.zero(principalAmount.getCurrency());
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Validate business rules
        validateLoanCreation();
    }

    // Factory method for creating new loans
    public static Loan create(
        LoanId id,
        CustomerId customerId,
        Money principalAmount,
        BigDecimal interestRate,
        Integer termInMonths,
        LoanType loanType,
        String purpose
    ) {
        Loan loan = new Loan(id, customerId, principalAmount, interestRate, termInMonths, loanType, purpose);
        
        loan.addDomainEvent(new LoanApplicationSubmittedEvent(
            id.getValue(),
            customerId.getValue(),
            principalAmount,
            loanType,
            purpose
        ));
        
        return loan;
    }

    // Business logic: Approve loan
    public void approve(String approvedBy) {
        if (this.status != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be approved");
        }
        
        Objects.requireNonNull(approvedBy, "Approved by cannot be null");
        
        this.status = LoanStatus.APPROVED;
        this.approvalDate = LocalDate.now();
        this.approvedBy = approvedBy;
        this.updatedAt = LocalDateTime.now();
        
        generateInstallments();
        
        addDomainEvent(new LoanApprovedEvent(
            this.id.getValue(), 
            this.customerId.getValue(),
            this.principalAmount,
            this.approvedBy
        ));
    }

    // Business logic: Reject loan
    public void reject(String reason) {
        if (this.status != LoanStatus.PENDING) {
            throw new IllegalStateException("Only pending loans can be rejected");
        }
        
        Objects.requireNonNull(reason, "Rejection reason cannot be null");
        
        this.status = LoanStatus.REJECTED;
        this.notes = reason;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanRejectedEvent(
            this.id.getValue(), 
            this.customerId.getValue(), 
            reason
        ));
    }

    // Business logic: Disburse loan
    public void disburse() {
        if (this.status != LoanStatus.APPROVED) {
            throw new IllegalStateException("Only approved loans can be disbursed");
        }
        
        this.status = LoanStatus.ACTIVE;
        this.disbursementDate = LocalDate.now();
        this.maturityDate = disbursementDate.plusMonths(termInMonths);
        this.outstandingBalance = this.principalAmount;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanDisbursedEvent(
            this.id.getValue(), 
            this.customerId.getValue(), 
            this.principalAmount
        ));
    }

    // Business logic: Make payment
    public void makePayment(Money paymentAmount, LocalDate paymentDate) {
        if (this.status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Payments can only be made on active loans");
        }
        
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        Objects.requireNonNull(paymentDate, "Payment date cannot be null");
        
        if (paymentAmount.isGreaterThan(this.outstandingBalance)) {
            throw new IllegalArgumentException("Payment amount cannot exceed outstanding balance");
        }
        
        this.outstandingBalance = this.outstandingBalance.subtract(paymentAmount);
        this.updatedAt = LocalDateTime.now();
        
        if (this.outstandingBalance.isZero()) {
            this.status = LoanStatus.PAID_OFF;
            addDomainEvent(new LoanPaidOffEvent(
                this.id.getValue(), 
                this.customerId.getValue()
            ));
        }
        
        addDomainEvent(new LoanPaymentMadeEvent(
            this.id.getValue(), 
            this.customerId.getValue(), 
            paymentAmount, 
            paymentDate
        ));
    }

    // Business logic: Mark as default
    public void markAsDefault() {
        if (this.status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Only active loans can be marked as default");
        }
        
        this.status = LoanStatus.DEFAULTED;
        this.updatedAt = LocalDateTime.now();
        
        addDomainEvent(new LoanDefaultedEvent(
            this.id.getValue(), 
            this.customerId.getValue()
        ));
    }

    // Business logic: Restructure loan
    public void restructure(BigDecimal newInterestRate, Integer newTermInMonths, String reason) {
        if (this.status != LoanStatus.ACTIVE) {
            throw new IllegalStateException("Only active loans can be restructured");
        }
        
        Objects.requireNonNull(newInterestRate, "New interest rate cannot be null");
        Objects.requireNonNull(newTermInMonths, "New term cannot be null");
        Objects.requireNonNull(reason, "Restructure reason cannot be null");
        
        this.interestRate = newInterestRate;
        this.termInMonths = newTermInMonths;
        this.notes = reason;
        this.updatedAt = LocalDateTime.now();
        
        // Regenerate installments with new terms
        generateInstallments();
        
        addDomainEvent(new LoanRestructuredEvent(
            this.id.getValue(),
            this.customerId.getValue(),
            newInterestRate,
            newTermInMonths,
            reason
        ));
    }

    // Business logic: Check if loan is overdue
    public boolean isOverdue() {
        return installments.stream()
            .anyMatch(installment -> 
                installment.getDueDate().isBefore(LocalDate.now()) && 
                installment.getStatus() == InstallmentStatus.PENDING
            );
    }

    // Business logic: Calculate total amount paid
    public Money getTotalPaid() {
        return installments.stream()
            .filter(installment -> installment.getStatus() == InstallmentStatus.PAID)
            .map(LoanInstallment::getTotalAmount)
            .reduce(Money.zero(principalAmount.getCurrency()), Money::add);
    }

    // Business logic: Calculate remaining payments
    public int getRemainingPayments() {
        return (int) installments.stream()
            .filter(installment -> installment.getStatus() == InstallmentStatus.PENDING)
            .count();
    }

    // Business logic: Get next payment due
    public LoanInstallment getNextPaymentDue() {
        return installments.stream()
            .filter(installment -> installment.getStatus() == InstallmentStatus.PENDING)
            .min((i1, i2) -> i1.getDueDate().compareTo(i2.getDueDate()))
            .orElse(null);
    }

    // Business logic: Calculate effective interest rate
    public BigDecimal getEffectiveAnnualRate() {
        // Simplified calculation - in real implementation would consider fees and compounding
        return interestRate;
    }

    // Private helper methods
    private void validateLoanCreation() {
        if (principalAmount.isZeroOrNegative()) {
            throw new IllegalArgumentException("Principal amount must be positive");
        }
        
        if (interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        
        if (termInMonths <= 0) {
            throw new IllegalArgumentException("Term in months must be positive");
        }
        
        if (termInMonths > 480) { // 40 years maximum
            throw new IllegalArgumentException("Term cannot exceed 40 years");
        }
        
        if (purpose == null || purpose.trim().isEmpty()) {
            throw new IllegalArgumentException("Loan purpose cannot be empty");
        }
    }

    private void generateInstallments() {
        installments.clear();
        
        if (approvalDate == null) {
            throw new IllegalStateException("Cannot generate installments without approval date");
        }
        
        Money monthlyPayment = calculateMonthlyPayment();
        LocalDate installmentDate = approvalDate.plusMonths(1);
        
        for (int i = 1; i <= termInMonths; i++) {
            Money principalForInstallment = calculatePrincipalForInstallment(i);
            Money interestForInstallment = calculateInterestForInstallment(i);
            
            LoanInstallment installment = LoanInstallment.create(
                this.id,
                i,
                installmentDate,
                principalForInstallment,
                interestForInstallment,
                monthlyPayment
            );
            
            installments.add(installment);
            installmentDate = installmentDate.plusMonths(1);
        }
    }

    private Money calculateMonthlyPayment() {
        if (interestRate.compareTo(BigDecimal.ZERO) == 0) {
            // Interest-free loan
            return principalAmount.divide(BigDecimal.valueOf(termInMonths));
        }
        
        double monthlyRate = interestRate.doubleValue() / 12;
        double numerator = monthlyRate * Math.pow(1 + monthlyRate, termInMonths);
        double denominator = Math.pow(1 + monthlyRate, termInMonths) - 1;
        double monthlyPayment = principalAmount.getAmount().doubleValue() * (numerator / denominator);
        
        return Money.of(BigDecimal.valueOf(monthlyPayment), principalAmount.getCurrency());
    }

    private Money calculatePrincipalForInstallment(int installmentNumber) {
        // Simplified calculation - in real implementation would use proper amortization schedule
        Money monthlyPayment = calculateMonthlyPayment();
        Money interestAmount = calculateInterestForInstallment(installmentNumber);
        return monthlyPayment.subtract(interestAmount);
    }

    private Money calculateInterestForInstallment(int installmentNumber) {
        // Simplified calculation - in real implementation would calculate based on remaining balance
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);
        return outstandingBalance.multiply(monthlyRate);
    }

    // Getters
    @Override
    public LoanId getId() { return id; }
    public CustomerId getCustomerId() { return customerId; }
    public Money getPrincipalAmount() { return principalAmount; }
    public Money getOutstandingBalance() { return outstandingBalance; }
    public BigDecimal getInterestRate() { return interestRate; }
    public Integer getTermInMonths() { return termInMonths; }
    public LoanType getLoanType() { return loanType; }
    public LoanStatus getStatus() { return status; }
    public LocalDate getApplicationDate() { return applicationDate; }
    public LocalDate getApprovalDate() { return approvalDate; }
    public LocalDate getDisbursementDate() { return disbursementDate; }
    public LocalDate getMaturityDate() { return maturityDate; }
    public List<LoanInstallment> getInstallments() { return new ArrayList<>(installments); }
    public LoanTerms getLoanTerms() { return loanTerms; }
    public String getPurpose() { return purpose; }
    public String getCollateralDescription() { return collateralDescription; }
    public String getApprovedBy() { return approvedBy; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    // Default constructor for JPA reconstruction
    protected Loan() {}

    // Public setters for reconstruction from persistence
    public void setId(LoanId id) { this.id = id; }
    public void setCustomerId(CustomerId customerId) { this.customerId = customerId; }
    public void setPrincipalAmount(Money principalAmount) { this.principalAmount = principalAmount; }
    public void setOutstandingBalance(Money outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public void setTermInMonths(Integer termInMonths) { this.termInMonths = termInMonths; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public void setApplicationDate(LocalDate applicationDate) { this.applicationDate = applicationDate; }
    public void setApprovalDate(LocalDate approvalDate) { this.approvalDate = approvalDate; }
    public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }
    public void setInstallments(List<LoanInstallment> installments) { this.installments = new ArrayList<>(installments); }
    public void setLoanTerms(LoanTerms loanTerms) { this.loanTerms = loanTerms; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public void setCollateralDescription(String collateralDescription) { this.collateralDescription = collateralDescription; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setVersion(Long version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loan loan = (Loan) o;
        return Objects.equals(id, loan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", principalAmount=" + principalAmount +
                ", loanType=" + loanType +
                ", status=" + status +
                '}';
    }
}