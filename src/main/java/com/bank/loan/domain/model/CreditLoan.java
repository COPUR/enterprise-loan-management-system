package com.bank.loan.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
     * Generate installments for the loan
     */
    public void generateInstallments() {
        if (numberOfInstallments == null || loanAmount == null || interestRate == null) {
            return;
        }
        
        // Calculate total amount with interest
        BigDecimal totalWithInterest = loanAmount.multiply(BigDecimal.ONE.add(interestRate));
        this.totalAmount = totalWithInterest;
        
        // Calculate installment amount
        BigDecimal installmentAmount = totalWithInterest.divide(BigDecimal.valueOf(numberOfInstallments), 2, BigDecimal.ROUND_HALF_UP);
        
        this.installments.clear();
        
        LocalDate currentDueDate = createDate != null ? createDate.plusMonths(1).withDayOfMonth(1) : LocalDate.now().plusMonths(1).withDayOfMonth(1);
        
        for (int i = 1; i <= numberOfInstallments; i++) {
            CreditLoanInstallment installment = CreditLoanInstallment.builder()
                .loanId(this.id)
                .installmentNumber(i)
                .amount(installmentAmount)
                .dueDate(currentDueDate)
                .isPaid(false)
                .paidAmount(BigDecimal.ZERO)
                .build();
            
            this.installments.add(installment);
            currentDueDate = currentDueDate.plusMonths(1);
        }
    }
    
    /**
     * Make a payment against the loan
     */
    public PaymentResult makePayment(BigDecimal paymentAmount) {
        BigDecimal remainingPayment = paymentAmount;
        int installmentsPaid = 0;
        BigDecimal totalAmountSpent = BigDecimal.ZERO;
        
        // Pay installments in order (earliest first)
        for (CreditLoanInstallment installment : installments) {
            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            
            if (!installment.getIsPaid()) {
                BigDecimal installmentPayment = remainingPayment.compareTo(installment.getAmount()) >= 0 
                    ? installment.getAmount() 
                    : remainingPayment;
                
                installment.setPaidAmount(installment.getPaidAmount().add(installmentPayment));
                
                if (installment.getPaidAmount().compareTo(installment.getAmount()) >= 0) {
                    installment.setIsPaid(true);
                    installment.setPaidDate(LocalDateTime.now());
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
            new Money(totalAmountSpent, "USD"), 
            isLoanFullyPaid);
    }
}