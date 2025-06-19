package com.bank.loanmanagement.domain.loan;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loan_installments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditLoanInstallment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private CreditLoan loan;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;
    
    @Column(nullable = false)
    private LocalDate dueDate;
    
    private LocalDate paymentDate;
    
    @Column(nullable = false)
    private Boolean isPaid;
    
    public void markAsPaid(BigDecimal paidAmount, LocalDate paymentDate) {
        this.paidAmount = paidAmount;
        this.paymentDate = paymentDate;
        this.isPaid = true;
    }
    
    public BigDecimal calculateRewardOrPenalty(LocalDate paymentDate) {
        if (paymentDate.isBefore(dueDate)) {
            // Reward: discount for early payment
            long daysBefore = java.time.temporal.ChronoUnit.DAYS.between(paymentDate, dueDate);
            return amount.multiply(BigDecimal.valueOf(0.001)).multiply(BigDecimal.valueOf(daysBefore));
        } else if (paymentDate.isAfter(dueDate)) {
            // Penalty: additional charge for late payment
            long daysAfter = java.time.temporal.ChronoUnit.DAYS.between(dueDate, paymentDate);
            return amount.multiply(BigDecimal.valueOf(0.001)).multiply(BigDecimal.valueOf(daysAfter)).negate();
        }
        return BigDecimal.ZERO; // No reward or penalty if paid on due date
    }
    
    public BigDecimal getEffectiveAmount(LocalDate paymentDate) {
        BigDecimal rewardOrPenalty = calculateRewardOrPenalty(paymentDate);
        return amount.subtract(rewardOrPenalty); // Subtract because penalty is negative, reward is positive
    }
}