package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Domain event fired when a payment is initiated.
 */
@Value
@Builder
@With
public class PaymentInitiatedEvent {
    
    PaymentId paymentId;
    Long loanId;
    String amount;
    String currency;
    PaymentMethod paymentMethod;
    PaymentType paymentType;
    String initiatedBy;
    LocalDateTime occurredAt;
    
    public static PaymentInitiatedEvent create(
            PaymentId paymentId, 
            Long loanId, 
            String amount, 
            String currency, 
            PaymentMethod paymentMethod,
            PaymentType paymentType,
            String initiatedBy) {
        
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
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }
        if (paymentType == null) {
            throw new IllegalArgumentException("Payment type cannot be null");
        }
        if (initiatedBy == null || initiatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Initiated by cannot be null or empty");
        }
        
        return PaymentInitiatedEvent.builder()
                .paymentId(paymentId)
                .loanId(loanId)
                .amount(amount.trim())
                .currency(currency.trim())
                .paymentMethod(paymentMethod)
                .paymentType(paymentType)
                .initiatedBy(initiatedBy.trim())
                .occurredAt(LocalDateTime.now())
                .build();
    }
}