package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain event fired when an early payoff is initiated.
 */
@Value
@Builder
@With
public class EarlyPayoffInitiatedEvent {
    
    PaymentId paymentId;
    Long loanId;
    String currentBalance;
    String payoffAmount;
    String discountAmount;
    String currency;
    LocalDate payoffDate;
    String initiatedBy;
    String payoffQuoteId;
    LocalDateTime occurredAt;
    
    public static EarlyPayoffInitiatedEvent create(
            PaymentId paymentId, 
            Long loanId, 
            String currentBalance,
            String payoffAmount,
            String discountAmount,
            String currency,
            LocalDate payoffDate,
            String initiatedBy,
            String payoffQuoteId) {
        
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (currentBalance == null || currentBalance.trim().isEmpty()) {
            throw new IllegalArgumentException("Current balance cannot be null or empty");
        }
        if (payoffAmount == null || payoffAmount.trim().isEmpty()) {
            throw new IllegalArgumentException("Payoff amount cannot be null or empty");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (payoffDate == null) {
            throw new IllegalArgumentException("Payoff date cannot be null");
        }
        if (initiatedBy == null || initiatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Initiated by cannot be null or empty");
        }
        if (payoffQuoteId == null || payoffQuoteId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payoff quote ID cannot be null or empty");
        }
        
        return EarlyPayoffInitiatedEvent.builder()
                .paymentId(paymentId)
                .loanId(loanId)
                .currentBalance(currentBalance.trim())
                .payoffAmount(payoffAmount.trim())
                .discountAmount(discountAmount != null ? discountAmount.trim() : null)
                .currency(currency.trim())
                .payoffDate(payoffDate)
                .initiatedBy(initiatedBy.trim())
                .payoffQuoteId(payoffQuoteId.trim())
                .occurredAt(LocalDateTime.now())
                .build();
    }
}