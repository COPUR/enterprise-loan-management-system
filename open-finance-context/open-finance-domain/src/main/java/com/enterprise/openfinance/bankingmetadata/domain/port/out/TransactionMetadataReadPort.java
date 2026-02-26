package com.enterprise.openfinance.bankingmetadata.domain.port.out;

import com.enterprise.openfinance.bankingmetadata.domain.model.TransactionMetadata;

import java.util.List;

public interface TransactionMetadataReadPort {

    List<TransactionMetadata> findByAccountId(String accountId);
}
