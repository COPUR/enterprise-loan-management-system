package com.enterprise.openfinance.insurancequotes.domain.port.out;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteIdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface MotorQuoteIdempotencyPort {

    Optional<MotorQuoteIdempotencyRecord> find(String key, String tppId, Instant now);

    void save(MotorQuoteIdempotencyRecord record);
}
