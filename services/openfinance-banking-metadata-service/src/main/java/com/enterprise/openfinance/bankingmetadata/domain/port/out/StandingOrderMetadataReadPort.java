package com.enterprise.openfinance.bankingmetadata.domain.port.out;

import com.enterprise.openfinance.bankingmetadata.domain.model.StandingOrderMetadata;

import java.util.List;

public interface StandingOrderMetadataReadPort {

    List<StandingOrderMetadata> findByAccountId(String accountId);

    List<StandingOrderMetadata> findAll();
}
