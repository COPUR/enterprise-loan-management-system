package com.enterprise.openfinance.fxservices.domain.port.out;

import com.enterprise.openfinance.fxservices.domain.model.FxDeal;
import com.enterprise.openfinance.fxservices.domain.model.FxQuote;

public interface FxEventPort {

    void publishQuoteCreated(FxQuote quote);

    void publishDealBooked(FxDeal deal);
}
