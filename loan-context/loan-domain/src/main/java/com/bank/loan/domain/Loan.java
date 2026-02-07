package com.bank.loan.domain;

import com.bank.shared.kernel.domain.CustomerId;
import com.bank.shared.kernel.domain.AggregateRoot;
import com.bank.shared.kernel.domain.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Loan Aggregate Root
 * 
 * Represents a financial loan with its terms, payment schedule,
 * and business rules for loan lifecycle management.
 */
public class Loan extends AggregateRoot<LoanId> {
    
    private static final Money MIN_LOAN_AMOUNT = Money.aed(new BigDecimal("1000.00"));
    private static final Money MAX_LOAN_AMOUNT = Money.aed(new BigDecimal("500000.00"));
    private static final int MIN_INSTALLMENTS = 6;
    private static final int MAX_INSTALLMENTS = 60;
    
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
    private List<LoanInstallment> installments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Private constructor for JPA
    protected Loan() {
        this.installments = new ArrayList<>();
    }
    
    private Loan(LoanId loanId, CustomerId customerId, Money principalAmount,
                InterestRate interestRate, LoanTerm loanTerm) {
        this();
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
    
    private Loan(LoanId loanId, CustomerId customerId, Money principalAmount,
                InterestRate interestRate, LoanTerm loanTerm, boolean generateInstallments) {
        this(loanId, customerId, principalAmount, interestRate, loanTerm);
        
        if (generateInstallments) {
            validateInstallmentGenerationRules();
            generateInstallments();
        }
    }
    
    public static Loan create(LoanId loanId, CustomerId customerId, Money principalAmount,
                             InterestRate interestRate, LoanTerm loanTerm) {
        return new Loan(loanId, customerId, principalAmount, interestRate, loanTerm);
    }
    
    public static Loan createWithInstallments(LoanId loanId, CustomerId customerId, Money principalAmount,
                                            InterestRate interestRate, LoanTerm loanTerm) {
        return new Loan(loanId, customerId, principalAmount, interestRate, loanTerm, true);
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
    
    private void validateInstallmentGenerationRules() {
        if (principalAmount.compareTo(MIN_LOAN_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                String.format("Loan amount must be at least %s", MIN_LOAN_AMOUNT));
        }
        if (principalAmount.compareTo(MAX_LOAN_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                String.format("Loan amount cannot exceed %s", MAX_LOAN_AMOUNT));
        }
        if (loanTerm.getMonths() < MIN_INSTALLMENTS) {
            throw new IllegalArgumentException(
                String.format("Loan term must be at least %d months", MIN_INSTALLMENTS));
        }
        if (loanTerm.getMonths() > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException(
                String.format("Loan term cannot exceed %d months", MAX_INSTALLMENTS));
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
    
    /**
     * Process a payment against the loan
     * 
     * Refactored following GRASP principles:
     * - Single Responsibility: Each method has one clear purpose
     * - Low Coupling: Minimal dependencies between methods
     * - High Cohesion: All methods work together for payment processing
     * 
     * @param paymentAmount The amount to be paid
     * @return PaymentResult containing payment processing details
     */
    public PaymentResult makePayment(Money paymentAmount) {
        validatePaymentPreconditions(paymentAmount);
        
        PaymentDistribution distribution = distributePaymentAcrossInstallments(paymentAmount);
        updateLoanBalance(distribution);
        updateLoanStatus();
        publishPaymentEvents(distribution);
        
        return createPaymentResult(distribution);
    }

    /**
     * Validate all preconditions for payment processing
     * 
     * Single Responsibility: Only validates payment preconditions
     */
    private void validatePaymentPreconditions(Money paymentAmount) {
        validateLoanStatusForPayment();
        validatePaymentAmount(paymentAmount);
        validatePaymentAgainstBalance(paymentAmount);
    }

    /**
     * Validate loan status allows payments
     */
    private void validateLoanStatusForPayment() {
        if (!status.canAcceptPayments()) {
            throw new IllegalStateException(
                String.format("Loan cannot accept payments in current status: %s", status)
            );
        }
    }

    /**
     * Validate payment amount is positive
     */
    private void validatePaymentAmount(Money paymentAmount) {
        if (paymentAmount == null) {
            throw new IllegalArgumentException("Payment amount cannot be null");
        }
        if (paymentAmount.isNegative() || paymentAmount.isZero()) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
    }

    /**
     * Validate payment doesn't exceed outstanding balance
     */
    private void validatePaymentAgainstBalance(Money paymentAmount) {
        if (paymentAmount.compareTo(outstandingBalance) > 0) {
            throw new IllegalArgumentException(
                String.format("Payment amount (%s) cannot exceed outstanding balance (%s)", 
                    paymentAmount, outstandingBalance)
            );
        }
    }

    /**
     * Distribute payment across loan installments
     * 
     * High Cohesion: Focused on payment distribution logic
     */
    private PaymentDistribution distributePaymentAcrossInstallments(Money paymentAmount) {
        Money previousBalance = this.outstandingBalance;
        Money principalPayment = calculatePrincipalPayment(paymentAmount);
        Money interestPayment = calculateInterestPayment(paymentAmount);
        
        PaymentDistribution distribution = PaymentDistribution.builder()
            .totalPayment(paymentAmount)
            .principalPayment(principalPayment)
            .interestPayment(interestPayment)
            .feePayment(Money.zero(paymentAmount.getCurrency()))
            .previousBalance(previousBalance)
            .paymentDate(LocalDate.now())
            .build();
            
        distribution.validate();
        return distribution;
    }

    /**
     * Calculate principal portion of payment
     */
    private Money calculatePrincipalPayment(Money paymentAmount) {
        // Simplified: For now, entire payment goes to principal
        // In a more sophisticated system, this would account for
        // accrued interest, payment allocation rules, etc.
        return paymentAmount;
    }

    /**
     * Calculate interest portion of payment
     */
    private Money calculateInterestPayment(Money paymentAmount) {
        // Simplified: No interest calculation for now
        // In production, this would calculate accrued interest
        return Money.zero(paymentAmount.getCurrency());
    }

    /**
     * Update loan balance with payment distribution
     * 
     * Single Responsibility: Only updates financial balances
     */
    private void updateLoanBalance(PaymentDistribution distribution) {
        this.outstandingBalance = this.outstandingBalance.subtract(distribution.getPrincipalPayment());
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Update loan status based on current balance
     * 
     * Information Expert: Loan knows its own status rules
     */
    private void updateLoanStatus() {
        if (this.outstandingBalance.isZero()) {
            this.status = LoanStatus.FULLY_PAID;
        }
        // Could add other status transitions here (e.g., CURRENT, OVERDUE)
    }

    /**
     * Publish relevant domain events for payment processing
     * 
     * Low Coupling: Encapsulates event publishing logic
     */
    private void publishPaymentEvents(PaymentDistribution distribution) {
        // Publish payment made event
        addDomainEvent(new LoanPaymentMadeEvent(
            loanId, 
            customerId, 
            distribution.getTotalPayment(),
            distribution.getPreviousBalance(),
            outstandingBalance
        ));
        
        // Publish fully paid event if applicable
        if (this.outstandingBalance.isZero()) {
            addDomainEvent(new LoanFullyPaidEvent(loanId, customerId));
        }
    }

    /**
     * Create comprehensive payment result
     * 
     * Creator Pattern: Loan creates its own payment results
     */
    private PaymentResult createPaymentResult(PaymentDistribution distribution) {
        PaymentResult result = PaymentResult.builder()
            .loanId(loanId)
            .paymentId(PaymentId.generate())
            .paymentDistribution(distribution)
            .newOutstandingBalance(outstandingBalance)
            .loanStatus(status)
            .paymentProcessedAt(LocalDateTime.now())
            .success(true)
            .build();
            
        result.validate();
        return result;
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
        
        // Business rule: For very short-term loans (1 month), return principal only
        if (loanTerm.getMonths() == 1) {
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
    
    // Archive business logic methods
    public List<LoanInstallment> getInstallments() {
        return new ArrayList<>(installments);
    }
    
    public Money getTotalInstallmentAmount() {
        return installments.stream()
            .map(LoanInstallment::getAmount)
            .reduce(Money.zero(principalAmount.getCurrency()), Money::add);
    }
    
    public Money getTotalInterest() {
        return getTotalInstallmentAmount().subtract(principalAmount);
    }
    
    public Money getRemainingInstallmentAmount() {
        return installments.stream()
            .filter(installment -> !installment.isPaid())
            .map(LoanInstallment::getAmount)
            .reduce(Money.zero(principalAmount.getCurrency()), Money::add);
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
    
    private void generateInstallments() {
        Money monthlyPayment = calculateMonthlyPayment();
        LocalDate currentDueDate = applicationDate.plusMonths(1);
        
        for (int i = 1; i <= loanTerm.getMonths(); i++) {
            LoanInstallment installment = LoanInstallment.create(
                loanId, customerId, i, monthlyPayment, currentDueDate);
            installments.add(installment);
            currentDueDate = currentDueDate.plusMonths(1);
        }
    }
}
