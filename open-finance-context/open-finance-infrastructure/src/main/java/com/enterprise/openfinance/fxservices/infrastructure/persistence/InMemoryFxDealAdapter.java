package com.enterprise.openfinance.fxservices.infrastructure.persistence;

import com.enterprise.openfinance.fxservices.domain.model.FxDeal;
import com.enterprise.openfinance.fxservices.domain.port.out.FxDealPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryFxDealAdapter implements FxDealPort {

    private final ConcurrentHashMap<String, FxDeal> data = new ConcurrentHashMap<>();

    @Override
    public FxDeal save(FxDeal deal) {
        data.put(deal.dealId(), deal);
        return deal;
    }

    @Override
    public Optional<FxDeal> findById(String dealId) {
        return Optional.ofNullable(data.get(dealId));
    }
}
