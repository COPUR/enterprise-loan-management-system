package com.enterprise.openfinance.uc11.domain.port.out;

import com.enterprise.openfinance.uc11.domain.model.FxQuoteItemResult;

import java.time.Instant;
import java.util.Optional;

public interface FxCachePort {

    Optional<FxQuoteItemResult> getQuote(String key, Instant now);

    void putQuote(String key, FxQuoteItemResult result, Instant expiresAt);
}
