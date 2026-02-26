package com.enterprise.openfinance.fxservices.domain.port.out;

import com.enterprise.openfinance.fxservices.domain.model.FxIdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface FxIdempotencyPort {

    Optional<FxIdempotencyRecord> find(String key, String tppId, Instant now);

    void save(FxIdempotencyRecord record);
}
