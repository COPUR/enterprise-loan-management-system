package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Value object representing payment reversal details.
 */
@Value
@Builder
@With
public class PaymentReversalDetails {
    
    PaymentReversalId reversalId;
    PaymentId originalPaymentId;
    PaymentReversalReason reason;
    String reversalNote;
    String reversalReference;
    String reversedBy;
    LocalDateTime reversalTime;
    String reversalFee;
    String reversalCurrency;
    boolean isAuthorized;
    String authorizationCode;
    
    public static PaymentReversalDetails create(
            PaymentReversalId reversalId,
            PaymentId originalPaymentId,
            PaymentReversalReason reason,
            String reversalNote,
            String reversalReference,
            String reversedBy,
            String reversalFee,
            String reversalCurrency,
            boolean isAuthorized,
            String authorizationCode) {
        
        if (reversalId == null) {
            throw new IllegalArgumentException("Reversal ID cannot be null");
        }
        if (originalPaymentId == null) {
            throw new IllegalArgumentException("Original payment ID cannot be null");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Reversal reason cannot be null");
        }
        if (reversalReference == null || reversalReference.trim().isEmpty()) {
            throw new IllegalArgumentException("Reversal reference cannot be null or empty");
        }
        if (reversedBy == null || reversedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Reversed by cannot be null or empty");
        }
        if (reversalCurrency == null || reversalCurrency.trim().isEmpty()) {
            throw new IllegalArgumentException("Reversal currency cannot be null or empty");
        }
        if (isAuthorized && (authorizationCode == null || authorizationCode.trim().isEmpty())) {
            throw new IllegalArgumentException("Authorization code is required for authorized reversals");
        }
        
        return PaymentReversalDetails.builder()
                .reversalId(reversalId)
                .originalPaymentId(originalPaymentId)
                .reason(reason)
                .reversalNote(reversalNote != null ? reversalNote.trim() : null)
                .reversalReference(reversalReference.trim())
                .reversedBy(reversedBy.trim())
                .reversalTime(LocalDateTime.now())
                .reversalFee(reversalFee != null ? reversalFee.trim() : null)
                .reversalCurrency(reversalCurrency.trim())
                .isAuthorized(isAuthorized)
                .authorizationCode(authorizationCode != null ? authorizationCode.trim() : null)
                .build();
    }
    
    public boolean hasReversalFee() {
        return reversalFee != null && !reversalFee.isEmpty();
    }
}