package com.enterprise.openfinance.uc10.domain.port.out;

import com.enterprise.openfinance.uc10.domain.model.MotorQuoteIdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface MotorQuoteIdempotencyPort {

    Optional<MotorQuoteIdempotencyRecord> find(String key, String tppId, Instant now);

    void save(MotorQuoteIdempotencyRecord record);
}
