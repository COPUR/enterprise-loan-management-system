package com.enterprise.openfinance.fxservices.infrastructure.event;

import com.enterprise.openfinance.fxservices.domain.model.FxDeal;
import com.enterprise.openfinance.fxservices.domain.model.FxQuote;
import com.enterprise.openfinance.fxservices.domain.port.out.FxEventPort;
import org.springframework.stereotype.Component;

@Component
public class NoOpFxEventAdapter implements FxEventPort {

    @Override
    public void publishQuoteCreated(FxQuote quote) {
    }

    @Override
    public void publishDealBooked(FxDeal deal) {
    }
}
