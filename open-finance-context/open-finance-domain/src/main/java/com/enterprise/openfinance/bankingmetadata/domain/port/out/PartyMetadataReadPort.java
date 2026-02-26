package com.enterprise.openfinance.bankingmetadata.domain.port.out;

import com.enterprise.openfinance.bankingmetadata.domain.model.PartyMetadata;

import java.util.List;

public interface PartyMetadataReadPort {

    List<PartyMetadata> findByAccountId(String accountId);
}
