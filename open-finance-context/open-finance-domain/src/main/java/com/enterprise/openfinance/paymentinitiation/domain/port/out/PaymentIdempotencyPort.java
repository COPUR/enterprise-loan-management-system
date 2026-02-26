package com.enterprise.openfinance.paymentinitiation.domain.port.out;

import com.enterprise.openfinance.paymentinitiation.domain.model.IdempotencyRecord;

import java.time.Instant;
import java.util.Optional;

public interface PaymentIdempotencyPort {
    Optional<IdempotencyRecord> find(String idempotencyKey, String tppId, Instant now);

    void save(IdempotencyRecord record);
}
