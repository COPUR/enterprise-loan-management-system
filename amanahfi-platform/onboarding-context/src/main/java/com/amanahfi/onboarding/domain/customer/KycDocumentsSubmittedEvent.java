package com.amanahfi.onboarding.domain.customer;

import java.time.LocalDateTime;

/**
 * Domain Event: KYC Documents Submitted
 * Published when customer submits KYC documents
 */
public record KycDocumentsSubmittedEvent(
    String customerId,
    int documentCount,
    LocalDateTime occurredAt
) {
    public KycDocumentsSubmittedEvent(String customerId, int documentCount) {
        this(customerId, documentCount, LocalDateTime.now());
    }
}