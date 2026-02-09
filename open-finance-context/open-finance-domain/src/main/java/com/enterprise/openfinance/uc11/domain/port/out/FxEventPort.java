package com.enterprise.openfinance.uc11.domain.port.out;

import com.enterprise.openfinance.uc11.domain.model.FxDeal;
import com.enterprise.openfinance.uc11.domain.model.FxQuote;

public interface FxEventPort {

    void publishQuoteCreated(FxQuote quote);

    void publishDealBooked(FxDeal deal);
}
