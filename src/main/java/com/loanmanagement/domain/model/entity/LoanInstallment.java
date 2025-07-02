package com.loanmanagement.domain.model.entity;

import com.loanmanagement.domain.model.value.Money;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

public class LoanInstallment {
    private static final BigDecimal DAILY_RATE = new BigDecimal("0.001");
    
    private Long id;
    private final Long loanId;
    private final Money amount;
    private Money paidAmount;
    private final LocalDate dueDate;
    private LocalDate paymentDate;
    private boolean isPaid;
    
    public LoanInstallment(Long id, Long loanId, Money amount, LocalDate dueDate) {
        this.id = id;
        this.loanId = loanId;
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = false;
    }
    
    public Money calculatePaymentAmount(LocalDate paymentDate) {
        if (paymentDate.isBefore(dueDate)) {
            return calculateWithEarlyPaymentDiscount(paymentDate);
        } else if (paymentDate.isAfter(dueDate)) {
            return calculateWithLatePaymentPenalty(paymentDate);
        } else {
            return amount;
        }
    }
    
    private Money calculateWithEarlyPaymentDiscount(LocalDate paymentDate) {
        long daysEarly = ChronoUnit.DAYS.between(paymentDate, dueDate);
        BigDecimal discountRate = DAILY_RATE.multiply(BigDecimal.valueOf(daysEarly));
        Money discount = amount.multiply(discountRate);
        return amount.subtract(discount);
    }
    
    private Money calculateWithLatePaymentPenalty(LocalDate paymentDate) {
        long daysLate = ChronoUnit.DAYS.between(dueDate, paymentDate);
        BigDecimal penaltyRate = DAILY_RATE.multiply(BigDecimal.valueOf(daysLate));
        Money penalty = amount.multiply(penaltyRate);
        return amount.add(penalty);
    }
    
    public void pay(LocalDate paymentDate) {
        if (isPaid) {
            throw new IllegalStateException("Installment is already paid");
        }
        
        this.paidAmount = calculatePaymentAmount(paymentDate);
        this.paymentDate = paymentDate;
        this.isPaid = true;
    }
    
    // Getters
    public Long getId() { return id; }
    public Long getLoanId() { return loanId; }
    public Money getAmount() { return amount; }
    public Money getPaidAmount() { return paidAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public boolean isPaid() { return isPaid; }
}