package com.enterprise.openfinance.uc04.domain.port.out;

import com.enterprise.openfinance.uc04.domain.model.AccountSchemeMetadata;

import java.util.Optional;

public interface AccountMetadataReadPort {

    Optional<AccountSchemeMetadata> findByAccountId(String accountId);
}
