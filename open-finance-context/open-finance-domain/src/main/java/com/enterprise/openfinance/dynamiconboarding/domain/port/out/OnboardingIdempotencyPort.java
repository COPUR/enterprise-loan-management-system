package com.enterprise.openfinance.dynamiconboarding.domain.port.out;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingIdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface OnboardingIdempotencyPort {

    Optional<OnboardingIdempotencyRecord> find(String key, String tppId, Instant now);

    void save(OnboardingIdempotencyRecord record);
}
