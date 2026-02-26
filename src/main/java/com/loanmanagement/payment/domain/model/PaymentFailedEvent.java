package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Domain event fired when a payment fails.
 */
@Value
@Builder
@With
public class PaymentFailedEvent {
    
    PaymentId paymentId;
    Long loanId;
    String amount;
    String currency;
    PaymentFailureReason failureReason;
    String failureMessage;
    String errorCode;
    LocalDateTime occurredAt;
    
    public static PaymentFailedEvent create(
            PaymentId paymentId, 
            Long loanId, 
            String amount, 
            String currency, 
            PaymentFailureReason failureReason,
            String failureMessage,
            String errorCode) {
        
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (loanId == null) {
            throw new IllegalArgumentException("Loan ID cannot be null");
        }
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be null or empty");
        }
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency cannot be null or empty");
        }
        if (failureReason == null) {
            throw new IllegalArgumentException("Failure reason cannot be null");
        }
        if (failureMessage == null || failureMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Failure message cannot be null or empty");
        }
        
        return PaymentFailedEvent.builder()
                .paymentId(paymentId)
                .loanId(loanId)
                .amount(amount.trim())
                .currency(currency.trim())
                .failureReason(failureReason)
                .failureMessage(failureMessage.trim())
                .errorCode(errorCode != null ? errorCode.trim() : null)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}