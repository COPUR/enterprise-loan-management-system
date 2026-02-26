package com.enterprise.openfinance.fxservices.domain.port.out;

import com.enterprise.openfinance.fxservices.domain.model.FxDeal;

import java.util.Optional;

public interface FxDealPort {

    FxDeal save(FxDeal deal);

    Optional<FxDeal> findById(String dealId);
}
