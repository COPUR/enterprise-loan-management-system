package com.enterprise.openfinance.uc15.domain.port.out;

import com.enterprise.openfinance.uc15.domain.model.AtmListResult;

import java.time.Instant;
import java.util.Optional;

public interface AtmCachePort {

    Optional<AtmListResult> getAtms(String key, Instant now);

    void putAtms(String key, AtmListResult result, Instant expiresAt);
}
