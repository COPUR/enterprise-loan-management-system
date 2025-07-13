package com.amanahfi.onboarding.domain.customer;

import java.time.LocalDateTime;

/**
 * Domain Event: Customer Registered
 * Published when a new customer registers in the system
 */
public record CustomerRegisteredEvent(
    String customerId,
    String emiratesId,
    String fullName,
    LocalDateTime occurredAt
) {
    public CustomerRegisteredEvent(String customerId, String emiratesId, String fullName) {
        this(customerId, emiratesId, fullName, LocalDateTime.now());
    }
}