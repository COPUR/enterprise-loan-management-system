package com.enterprise.openfinance.uc11.domain.port.out;

import com.enterprise.openfinance.uc11.domain.model.FxQuote;

import java.util.Optional;

public interface FxQuotePort {

    FxQuote save(FxQuote quote);

    Optional<FxQuote> findById(String quoteId);
}
