package com.enterprise.openfinance.fxservices.domain.port.out;

import com.enterprise.openfinance.fxservices.domain.model.FxQuote;

import java.util.Optional;

public interface FxQuotePort {

    FxQuote save(FxQuote quote);

    Optional<FxQuote> findById(String quoteId);
}
