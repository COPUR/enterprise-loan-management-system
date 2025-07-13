package com.amanahfi.onboarding.domain.customer;

import java.time.LocalDateTime;

/**
 * Domain Event: Customer Preferences Updated
 * Published when customer updates their preferences
 */
public record CustomerPreferencesUpdatedEvent(
    String customerId,
    boolean islamicBankingPreferred,
    LocalDateTime occurredAt
) {
    public CustomerPreferencesUpdatedEvent(String customerId, boolean islamicBankingPreferred) {
        this(customerId, islamicBankingPreferred, LocalDateTime.now());
    }
}