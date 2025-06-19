package com.bank.loanmanagement.domain.loan;

import com.bank.loanmanagement.domain.shared.Money;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// @Entity
@Table(name = "loan_installments_v2", indexes = {
    @Index(name = "idx_installment_loan", columnList = "loan_id"),
    @Index(name = "idx_installment_due_date", columnList = "dueDate"),
    @Index(name = "idx_installment_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer installmentNumber;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "principal_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "principal_currency"))
    })
    private Money principalAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "interest_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "interest_currency"))
    })
    private Money interestAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "total_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "total_currency"))
    })
    private Money totalAmount;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "paid_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "paid_currency"))
    })
    private Money paidAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstallmentStatus status;
    
    private LocalDateTime paidDate;
    private String paymentReference;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;
    
    public void markAsPaid(Money amount, String paymentReference) {
        if (this.status == InstallmentStatus.PAID) {
            throw new IllegalStateException("Installment is already paid");
        }
        
        this.paidAmount = amount;
        this.paidDate = LocalDateTime.now();
        this.paymentReference = paymentReference;
        
        if (amount.isGreaterThan(totalAmount) || amount.equals(totalAmount)) {
            this.status = InstallmentStatus.PAID;
        } else {
            this.status = InstallmentStatus.PARTIALLY_PAID;
        }
    }
    
    public boolean isOverdue() {
        return status == InstallmentStatus.PENDING && dueDate.isBefore(LocalDate.now());
    }
    
    public Money getRemainingAmount() {
        if (paidAmount == null) {
            return totalAmount;
        }
        return totalAmount.subtract(paidAmount);
    }
}