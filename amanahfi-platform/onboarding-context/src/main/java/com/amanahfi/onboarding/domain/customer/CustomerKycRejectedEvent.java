package com.amanahfi.onboarding.domain.customer;

import java.time.LocalDateTime;

/**
 * Domain Event: Customer KYC Rejected
 * Published when customer's KYC is rejected
 */
public record CustomerKycRejectedEvent(
    String customerId,
    String officerId,
    String reason,
    LocalDateTime occurredAt
) {
    public CustomerKycRejectedEvent(String customerId, String officerId, String reason) {
        this(customerId, officerId, reason, LocalDateTime.now());
    }
}