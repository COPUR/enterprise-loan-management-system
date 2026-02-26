package com.enterprise.openfinance.dynamiconboarding.domain.model;

public record OnboardingAccountItemResult(
        OnboardingAccount account,
        boolean cacheHit
) {

    public OnboardingAccountItemResult {
        if (account == null) {
            throw new IllegalArgumentException("account is required");
        }
    }

    public OnboardingAccountItemResult withCacheHit(boolean cacheHit) {
        return new OnboardingAccountItemResult(account, cacheHit);
    }
}
