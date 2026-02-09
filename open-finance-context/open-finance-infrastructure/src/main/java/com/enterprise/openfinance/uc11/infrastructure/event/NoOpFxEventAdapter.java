package com.enterprise.openfinance.uc11.infrastructure.event;

import com.enterprise.openfinance.uc11.domain.model.FxDeal;
import com.enterprise.openfinance.uc11.domain.model.FxQuote;
import com.enterprise.openfinance.uc11.domain.port.out.FxEventPort;
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
