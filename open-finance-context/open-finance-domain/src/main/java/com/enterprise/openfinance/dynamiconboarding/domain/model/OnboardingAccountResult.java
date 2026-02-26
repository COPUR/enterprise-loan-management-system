package com.enterprise.openfinance.dynamiconboarding.domain.model;

public record OnboardingAccountResult(
        OnboardingAccount account,
        boolean idempotencyReplay
) {

    public OnboardingAccountResult {
        if (account == null) {
            throw new IllegalArgumentException("account is required");
        }
    }
}
