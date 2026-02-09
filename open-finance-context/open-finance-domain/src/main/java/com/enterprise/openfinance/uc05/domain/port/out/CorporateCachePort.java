package com.enterprise.openfinance.uc05.domain.port.out;

import com.enterprise.openfinance.uc05.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateTransactionSnapshot;

import java.time.Instant;
import java.util.Optional;

public interface CorporateCachePort {

    Optional<CorporateAccountListResult> getAccounts(String key, Instant now);

    void putAccounts(String key, CorporateAccountListResult value, Instant expiresAt);

    Optional<CorporateBalanceListResult> getBalances(String key, Instant now);

    void putBalances(String key, CorporateBalanceListResult value, Instant expiresAt);

    Optional<CorporatePagedResult<CorporateTransactionSnapshot>> getTransactions(String key, Instant now);

    void putTransactions(String key, CorporatePagedResult<CorporateTransactionSnapshot> value, Instant expiresAt);
}
