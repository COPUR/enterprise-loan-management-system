package com.enterprise.openfinance.uc11.domain.port.out;

import com.enterprise.openfinance.uc11.domain.model.FxDeal;

import java.util.Optional;

public interface FxDealPort {

    FxDeal save(FxDeal deal);

    Optional<FxDeal> findById(String dealId);
}
