
package com.loanmanagement.domain.event;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentMade extends DomainEvent {
    private final Long loanId;
    private final Long customerId;
    private final Integer installmentsPaid;
    private final BigDecimal totalAmountPaid;
    private final BigDecimal discountAmount;
    private final BigDecimal penaltyAmount;
    private final boolean loanFullyPaid;
    private final LocalDate paymentDate;
    
    public PaymentMade(
        Long loanId,
        Long customerId,
        Integer installmentsPaid,
        BigDecimal totalAmountPaid,
        BigDecimal discountAmount,
        BigDecimal penaltyAmount,
        boolean loanFullyPaid,
        LocalDate paymentDate
    ) {
        super(loanId.toString());
        this.loanId = loanId;
        this.customerId = customerId;
        this.installmentsPaid = installmentsPaid;
        this.totalAmountPaid = totalAmountPaid;
        this.discountAmount = discountAmount;
        this.penaltyAmount = penaltyAmount;
        this.loanFullyPaid = loanFullyPaid;
        this.paymentDate = paymentDate;
    }
    
    // Getters
    public Long getLoanId() { return loanId; }
    public Long getCustomerId() { return customerId; }
    public Integer getInstallmentsPaid() { return installmentsPaid; }
    public BigDecimal getTotalAmountPaid() { return totalAmountPaid; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public boolean isLoanFullyPaid() { return loanFullyPaid; }
    public LocalDate getPaymentDate() { return paymentDate; }
}