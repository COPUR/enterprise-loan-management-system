package com.enterprise.openfinance.dynamiconboarding.domain.port.out;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccountItemResult;

import java.time.Instant;
import java.util.Optional;

public interface OnboardingCachePort {

    Optional<OnboardingAccountItemResult> getAccount(String key, Instant now);

    void putAccount(String key, OnboardingAccountItemResult result, Instant expiresAt);
}
