package com.enterprise.openfinance.uc04.domain.port.out;

import com.enterprise.openfinance.uc04.domain.model.TransactionMetadata;

import java.util.List;

public interface TransactionMetadataReadPort {

    List<TransactionMetadata> findByAccountId(String accountId);
}
