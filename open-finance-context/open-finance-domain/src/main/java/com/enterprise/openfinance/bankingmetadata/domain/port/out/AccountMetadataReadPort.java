package com.enterprise.openfinance.bankingmetadata.domain.port.out;

import com.enterprise.openfinance.bankingmetadata.domain.model.AccountSchemeMetadata;

import java.util.Optional;

public interface AccountMetadataReadPort {

    Optional<AccountSchemeMetadata> findByAccountId(String accountId);
}
