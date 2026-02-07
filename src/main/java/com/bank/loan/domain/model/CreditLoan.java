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
 * Credit Loan entity for integration tests
 * Simplified version focused on credit operations
 */
@Entity
@Table(name = "credit_loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLoan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "loan_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal loanAmount;
    
    @Column(name = "interest_rate", precision = 8, scale = 4, nullable = false)
    private BigDecimal interestRate;
    
    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;
    
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "create_date")
    private LocalDate createDate;
    
    @Column(name = "is_paid")
    private Boolean isPaid;
    
    @OneToMany(mappedBy = "loanId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("installmentNumber ASC")
    private List<CreditLoanInstallment> installments = new ArrayList<>();
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (createDate == null) {
            createDate = LocalDate.now();
        }
        if (isPaid == null) {
            isPaid = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Factory method to create a new credit loan with basic validation
     */
    public static CreditLoan create(Long customerId,
                                    BigDecimal loanAmount,
                                    BigDecimal interestRate,
                                    Integer numberOfInstallments) {
        Objects.requireNonNull(customerId, "Customer ID cannot be null");
        Objects.requireNonNull(loanAmount, "Loan amount cannot be null");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null");
        Objects.requireNonNull(numberOfInstallments, "Number of installments cannot be null");

        if (loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Loan amount must be positive");
        }
        if (interestRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
        if (numberOfInstallments <= 0) {
            throw new IllegalArgumentException("Number of installments must be positive");
        }

        CreditLoan loan = CreditLoan.builder()
            .customerId(customerId)
            .loanAmount(scaleCurrency(loanAmount))
            .interestRate(interestRate)
            .numberOfInstallments(numberOfInstallments)
            .createDate(LocalDate.now())
            .isPaid(false)
            .build();

        loan.generateInstallments();
        return loan;
    }
    
    /**
     * Generate installments for the loan
     */
    public void generateInstallments() {
        if (numberOfInstallments == null || loanAmount == null || interestRate == null) {
            return;
        }

        if (numberOfInstallments <= 0 || loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        // Calculate total amount with interest
        BigDecimal totalWithInterest = calculateTotalWithInterest();
        this.totalAmount = totalWithInterest;
        
        // Calculate installment amount
        BigDecimal installmentAmount = totalWithInterest.divide(
            BigDecimal.valueOf(numberOfInstallments),
            2,
            RoundingMode.HALF_UP
        );

        if (this.installments == null) {
            this.installments = new ArrayList<>();
        } else {
            this.installments.clear();
        }
        
        LocalDate currentDueDate = (createDate != null ? createDate : LocalDate.now())
            .plusMonths(1)
            .withDayOfMonth(1);
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            CreditLoanInstallment installment = CreditLoanInstallment.create(
                this.id,
                i,
                installmentAmount,
                currentDueDate
            );
            
            this.installments.add(installment);
            currentDueDate = currentDueDate.plusMonths(1);
        }
    }
    
    /**
     * Make a payment against the loan
     */
    public PaymentResult makePayment(BigDecimal paymentAmount) {
        Objects.requireNonNull(paymentAmount, "Payment amount cannot be null");
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        if (installments == null || installments.isEmpty()) {
            return PaymentResult.failure();
        }

        BigDecimal remainingPayment = paymentAmount;
        int installmentsPaid = 0;
        BigDecimal totalAmountSpent = BigDecimal.ZERO;
        
        // Pay installments in order (earliest first)
        for (CreditLoanInstallment installment : installments) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            if (!Boolean.TRUE.equals(installment.getIsPaid())) {
                BigDecimal remainingAmount = installment.getRemainingAmount();
                if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal installmentPayment = remainingPayment.compareTo(remainingAmount) >= 0
                    ? remainingAmount
                    : remainingPayment;

                installment.applyPayment(installmentPayment, LocalDateTime.now());

                if (Boolean.TRUE.equals(installment.getIsPaid())) {
                    installmentsPaid++;
                }
                
                totalAmountSpent = totalAmountSpent.add(installmentPayment);
                remainingPayment = remainingPayment.subtract(installmentPayment);
            }
        }
        
        // Check if loan is fully paid
        boolean isLoanFullyPaid = installments.stream().allMatch(CreditLoanInstallment::getIsPaid);
        if (isLoanFullyPaid) {
            this.isPaid = true;
        }
        
        this.updatedAt = LocalDateTime.now();
        
        return new PaymentResult(installmentsPaid, 
            Money.of(totalAmountSpent, "USD"), 
            isLoanFullyPaid);
    }

    private BigDecimal calculateTotalWithInterest() {
        BigDecimal total = loanAmount.multiply(BigDecimal.ONE.add(interestRate));
        return scaleCurrency(total);
    }

    private static BigDecimal scaleCurrency(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
