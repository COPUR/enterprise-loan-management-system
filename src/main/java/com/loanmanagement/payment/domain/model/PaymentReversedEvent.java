package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Domain event fired when a payment is reversed.
 */
@Value
@Builder
@With
public class PaymentReversedEvent {
    
    PaymentId paymentId;
    PaymentReversalId reversalId;
    Long loanId;
    String amount;
    String currency;
    PaymentReversalReason reversalReason;
    String reversalNote;
    String reversedBy;
    PaymentReversalDetails reversalDetails;
    LocalDateTime occurredAt;
    
    public static PaymentReversedEvent create(
            PaymentId paymentId, 
            PaymentReversalId reversalId,
            Long loanId, 
            String amount, 
            String currency, 
            PaymentReversalReason reversalReason,
            String reversalNote,
            String reversedBy,
            PaymentReversalDetails reversalDetails) {
        
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (reversalId == null) {
            throw new IllegalArgumentException("Reversal ID cannot be null");
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
        if (reversalReason == null) {
            throw new IllegalArgumentException("Reversal reason cannot be null");
        }
        if (reversedBy == null || reversedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Reversed by cannot be null or empty");
        }
        
        return PaymentReversedEvent.builder()
                .paymentId(paymentId)
                .reversalId(reversalId)
                .loanId(loanId)
                .amount(amount.trim())
                .currency(currency.trim())
                .reversalReason(reversalReason)
                .reversalNote(reversalNote != null ? reversalNote.trim() : null)
                .reversedBy(reversedBy.trim())
                .reversalDetails(reversalDetails)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}