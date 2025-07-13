package com.bank.loan.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Core Loan entity representing a loan in the system
 */
@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "loan_reference", unique = true)
    private String loanReference;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "principal_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency"))
    })
    private Money principalAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "rate", column = @Column(name = "interest_rate", precision = 8, scale = 4))
    })
    private InterestRate interestRate;
    
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "total_currency"))
    })
    private Money totalAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "monthly_payment", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "monthly_currency"))
    })
    private Money monthlyPayment;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "outstanding_balance", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "balance_currency"))
    })
    private Money outstandingBalance;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type")
    private LoanType loanType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_status", nullable = false)
    private LoanStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "loan_purpose")
    private LoanPurpose purpose;
    
    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;
    
    @Column(name = "maturity_date")
    private LocalDate maturityDate;
    
    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;
    
    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;
    
    @Column(name = "paid_installments")
    private Integer paidInstallments;
    
    @Column(name = "remaining_installments")
    private Integer remainingInstallments;
    
    @Column(name = "is_paid")
    private Boolean isPaid;
    
    @Column(name = "collateral_description")
    private String collateralDescription;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "collateral_value", precision = 15, scale = 2)),
        @AttributeOverride(name = "currency", column = @Column(name = "collateral_currency"))
    })
    private Money collateralValue;
    
    @OneToMany(mappedBy = "loanId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("installmentNumber ASC")
    private List<LoanInstallment> installments = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    @Version
    private Long version;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = LoanStatus.CREATED;
        }
        if (isPaid == null) {
            isPaid = false;
        }
        if (paidInstallments == null) {
            paidInstallments = 0;
        }
        if (remainingInstallments == null) {
            remainingInstallments = termMonths;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Factory method to create a loan (from LoanTest requirements)
     */
    public static Loan create(Long id, Long customerId, Money principal, InterestRate rate, Integer installmentCount) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(principal, "Principal amount cannot be null");
        Objects.requireNonNull(rate, "Interest rate cannot be null");
        Objects.requireNonNull(installmentCount, "Installment count cannot be null");
        
        // Calculate total amount with interest (simple interest for now)
        Money totalAmount = principal.add(rate.calculateInterest(principal));
        Money monthlyPayment = totalAmount.divide(installmentCount);
        
        Loan loan = Loan.builder()
            .id(id)
            .customerId(customerId)
            .principalAmount(principal)
            .interestRate(rate)
            .termMonths(installmentCount)
            .totalAmount(totalAmount)
            .monthlyPayment(monthlyPayment)
            .outstandingBalance(totalAmount)
            .status(LoanStatus.CREATED)
            .isPaid(false)
            .paidInstallments(0)
            .remainingInstallments(installmentCount)
            .disbursementDate(LocalDate.now())
            .maturityDate(LocalDate.now().plusMonths(installmentCount))
            .firstPaymentDate(LocalDate.now().plusMonths(1))
            .build();
        
        // Generate installments
        loan.generateInstallments();
        
        return loan;
    }
    
    /**
     * Generate installments for the loan
     */
    public void generateInstallments() {
        this.installments.clear();
        
        if (termMonths == null || monthlyPayment == null) {
            return;
        }
        
        Money remainingBalance = totalAmount;
        LocalDate currentDueDate = firstPaymentDate != null ? firstPaymentDate : LocalDate.now().plusMonths(1);
        
        for (int i = 1; i <= termMonths; i++) {
            // For the last installment, use the remaining balance to avoid rounding errors
            Money installmentAmount = (i == termMonths) ? remainingBalance : monthlyPayment;
            remainingBalance = remainingBalance.subtract(installmentAmount);
            
            LoanInstallment installment = LoanInstallment.create(
                this.id, 
                i, 
                installmentAmount, 
                installmentAmount, // For simplicity, treating whole payment as principal
                Money.zero(installmentAmount.getCurrency()), 
                currentDueDate, 
                remainingBalance
            );
            
            this.installments.add(installment);
            currentDueDate = currentDueDate.plusMonths(1);
        }
    }
    
    /**
     * Make a payment against the loan (from test requirements)
     */
    public PaymentResult makePayment(Money paymentAmount, LocalDate paymentDate) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        Objects.requireNonNull(paymentDate, "Payment date cannot be null");
        
        if (!status.canAcceptPayments()) {
            throw new IllegalStateException("Loan cannot accept payments in current status: " + status);
        }
        
        Money remainingPayment = paymentAmount;
        int installmentsPaid = 0;
        Money totalAmountSpent = Money.zero(paymentAmount.getCurrency());
        
        // Pay installments in order (earliest first)
        for (LoanInstallment installment : installments) {
            if (remainingPayment.isZero() || remainingPayment.isNegative()) {
                break;
            }
            
            if (installment.canAcceptPayment()) {
                Money installmentPayment = remainingPayment.isGreaterThan(installment.getRemainingAmount()) 
                    ? installment.getRemainingAmount() 
                    : remainingPayment;
                
                Money overpayment = installment.makePayment(installmentPayment, "LOAN-PAY-" + System.currentTimeMillis());
                
                totalAmountSpent = totalAmountSpent.add(installmentPayment);
                remainingPayment = remainingPayment.subtract(installmentPayment).add(overpayment);
                
                if (installment.isPaid()) {
                    installmentsPaid++;
                    this.paidInstallments = (this.paidInstallments != null ? this.paidInstallments : 0) + 1;
                    this.remainingInstallments = this.termMonths - this.paidInstallments;
                    this.lastPaymentDate = paymentDate;
                }
            }
        }
        
        // Update outstanding balance
        this.outstandingBalance = this.outstandingBalance.subtract(totalAmountSpent);
        
        // Check if loan is fully paid
        if (this.paidInstallments != null && this.paidInstallments.equals(this.termMonths)) {
            this.isPaid = true;
            this.status = LoanStatus.FULLY_PAID;
        } else if (this.paidInstallments != null && this.paidInstallments > 0) {
            this.status = LoanStatus.ACTIVE;
        }
        
        this.updatedAt = LocalDateTime.now();
        
        return new PaymentResult(installmentsPaid, totalAmountSpent, this.isPaid);
    }
    
    /**
     * Get loan amount (principal + interest)
     */
    public Money getLoanAmount() {
        return totalAmount;
    }
    
    /**
     * Get loan ID for domain operations
     */
    public LoanId getLoanId() {
        return id != null ? LoanId.fromLong(id) : LoanId.generate();
    }
    
    /**
     * Calculate total interest amount
     */
    public Money getTotalInterest() {
        if (totalAmount != null && principalAmount != null) {
            return totalAmount.subtract(principalAmount);
        }
        return Money.zero(principalAmount != null ? principalAmount.getCurrency() : "USD");
    }
    
    /**
     * Get loan-to-value ratio (for secured loans)
     */
    public BigDecimal getLoanToValueRatio() {
        if (collateralValue == null || collateralValue.isZero() || principalAmount == null) {
            return BigDecimal.ZERO;
        }
        return principalAmount.getAmount().divide(collateralValue.getAmount(), 4, RoundingMode.HALF_UP);
    }
    
    /**
     * Check if loan is overdue
     */
    public boolean isOverdue() {
        return installments.stream().anyMatch(LoanInstallment::isOverdue);
    }
    
    /**
     * Get number of overdue installments
     */
    public long getOverdueInstallments() {
        return installments.stream().mapToLong(i -> i.isOverdue() ? 1 : 0).sum();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Loan loan = (Loan) o;
        return Objects.equals(loanReference, loan.loanReference) || Objects.equals(id, loan.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(loanReference != null ? loanReference : id);
    }
}