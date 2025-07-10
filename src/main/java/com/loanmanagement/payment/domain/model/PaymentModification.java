package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Value object representing a payment modification.
 */
@Value
@Builder
@With
public class PaymentModification {
    
    PaymentModificationId id;
    PaymentId paymentId;
    PaymentModificationType type;
    String originalValue;
    String newValue;
    String reason;
    String modifiedBy;
    LocalDateTime modifiedAt;
    
    public static PaymentModification create(
            PaymentModificationId id,
            PaymentId paymentId,
            PaymentModificationType type,
            String originalValue,
            String newValue,
            String reason,
            String modifiedBy) {
        
        if (id == null) {
            throw new IllegalArgumentException("Payment modification ID cannot be null");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Modification type cannot be null");
        }
        if (originalValue == null || originalValue.trim().isEmpty()) {
            throw new IllegalArgumentException("Original value cannot be null or empty");
        }
        if (newValue == null || newValue.trim().isEmpty()) {
            throw new IllegalArgumentException("New value cannot be null or empty");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        if (modifiedBy == null || modifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Modified by cannot be null or empty");
        }
        if (originalValue.equals(newValue)) {
            throw new IllegalArgumentException("Original value and new value cannot be the same");
        }
        
        return PaymentModification.builder()
                .id(id)
                .paymentId(paymentId)
                .type(type)
                .originalValue(originalValue.trim())
                .newValue(newValue.trim())
                .reason(reason.trim())
                .modifiedBy(modifiedBy.trim())
                .modifiedAt(LocalDateTime.now())
                .build();
    }
}