package com.enterprise.openfinance.fxservices.domain.port.out;

import com.enterprise.openfinance.fxservices.domain.model.FxQuoteItemResult;

import java.time.Instant;
import java.util.Optional;

public interface FxCachePort {

    Optional<FxQuoteItemResult> getQuote(String key, Instant now);

    void putQuote(String key, FxQuoteItemResult result, Instant expiresAt);
}
