package com.enterprise.openfinance.uc11.infrastructure.persistence;

import com.enterprise.openfinance.uc11.domain.model.FxDeal;
import com.enterprise.openfinance.uc11.domain.port.out.FxDealPort;
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
