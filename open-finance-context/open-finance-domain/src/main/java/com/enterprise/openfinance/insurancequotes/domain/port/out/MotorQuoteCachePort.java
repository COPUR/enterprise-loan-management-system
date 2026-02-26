package com.enterprise.openfinance.insurancequotes.domain.port.out;

import com.enterprise.openfinance.insurancequotes.domain.model.MotorQuoteItemResult;

import java.time.Instant;
import java.util.Optional;

public interface MotorQuoteCachePort {

    Optional<MotorQuoteItemResult> getQuote(String key, Instant now);

    void putQuote(String key, MotorQuoteItemResult result, Instant expiresAt);
}
