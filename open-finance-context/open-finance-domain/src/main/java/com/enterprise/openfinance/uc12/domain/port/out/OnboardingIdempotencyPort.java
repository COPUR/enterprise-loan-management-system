package com.enterprise.openfinance.uc12.domain.port.out;

import com.enterprise.openfinance.uc12.domain.model.OnboardingIdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface OnboardingIdempotencyPort {

    Optional<OnboardingIdempotencyRecord> find(String key, String tppId, Instant now);

    void save(OnboardingIdempotencyRecord record);
}
