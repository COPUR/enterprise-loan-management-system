package com.enterprise.openfinance.uc12.domain.model;

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
