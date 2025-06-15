
package com.bank.loanmanagement.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long installmentId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;
    
    @NotNull(message = "Installment number is required")
    @Min(value = 1, message = "Installment number must be positive")
    @Column(nullable = false)
    private Integer installmentNumber;
    
    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "0.00", message = "Principal amount must be non-negative")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal principalAmount;
    
    @NotNull(message = "Interest amount is required")
    @DecimalMin(value = "0.00", message = "Interest amount must be non-negative")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal interestAmount;
    
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.00", message = "Total amount must be non-negative")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @NotNull(message = "Due date is required")
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallmentStatus status = InstallmentStatus.PENDING;
    
    @Column
    private LocalDate paidDate;
    
    @DecimalMin(value = "0.00", message = "Paid amount must be non-negative")
    @Digits(integer = 10, fraction = 2)
    @Column(precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum InstallmentStatus {
        PENDING, PARTIALLY_PAID, PAID, OVERDUE
    }
    
    // Business methods
    public boolean isOverdue() {
        return status == InstallmentStatus.PENDING && 
               dueDate.isBefore(LocalDate.now());
    }
    
    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(paidAmount);
    }
    
    public void makePayment(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        BigDecimal remainingAmount = getRemainingAmount();
        if (amount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds remaining balance");
        }
        
        this.paidAmount = this.paidAmount.add(amount);
        
        if (this.paidAmount.compareTo(this.totalAmount) >= 0) {
            this.status = InstallmentStatus.PAID;
            this.paidDate = LocalDate.now();
        } else {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }
    }
}
