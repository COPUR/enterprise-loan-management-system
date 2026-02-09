package com.enterprise.openfinance.uc04.domain.port.out;

import com.enterprise.openfinance.uc04.domain.model.StandingOrderMetadata;

import java.util.List;

public interface StandingOrderMetadataReadPort {

    List<StandingOrderMetadata> findByAccountId(String accountId);

    List<StandingOrderMetadata> findAll();
}
