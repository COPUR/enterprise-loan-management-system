package com.loanmanagement.payment.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

/**
 * Value object representing a payment note.
 */
@Value
@Builder
@With
public class PaymentNote {
    
    PaymentNoteId id;
    PaymentId paymentId;
    String content;
    String createdBy;
    LocalDateTime createdAt;
    boolean isInternal;
    
    public static PaymentNote create(
            PaymentNoteId id,
            PaymentId paymentId,
            String content,
            String createdBy,
            boolean isInternal) {
        
        if (id == null) {
            throw new IllegalArgumentException("Payment note ID cannot be null");
        }
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment ID cannot be null");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (createdBy == null || createdBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Created by cannot be null or empty");
        }
        if (content.length() > 1000) {
            throw new IllegalArgumentException("Content cannot exceed 1000 characters");
        }
        
        return PaymentNote.builder()
                .id(id)
                .paymentId(paymentId)
                .content(content.trim())
                .createdBy(createdBy.trim())
                .createdAt(LocalDateTime.now())
                .isInternal(isInternal)
                .build();
    }
    
    public boolean isPublic() {
        return !isInternal;
    }
}