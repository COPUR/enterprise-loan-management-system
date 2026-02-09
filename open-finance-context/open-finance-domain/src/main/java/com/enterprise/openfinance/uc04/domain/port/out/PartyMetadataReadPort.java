package com.enterprise.openfinance.uc04.domain.port.out;

import com.enterprise.openfinance.uc04.domain.model.PartyMetadata;

import java.util.List;

public interface PartyMetadataReadPort {

    List<PartyMetadata> findByAccountId(String accountId);
}
