package com.amanahfi.onboarding.domain.customer;

import java.time.LocalDateTime;

/**
 * Domain Event: Customer KYC Approved
 * Published when customer's KYC is approved and they become active
 */
public record CustomerKycApprovedEvent(
    String customerId,
    String officerId,
    String notes,
    LocalDateTime occurredAt
) {
    public CustomerKycApprovedEvent(String customerId, String officerId, String notes) {
        this(customerId, officerId, notes, LocalDateTime.now());
    }
}