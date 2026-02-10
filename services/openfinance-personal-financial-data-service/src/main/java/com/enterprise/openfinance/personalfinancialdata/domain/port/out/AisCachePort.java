package com.enterprise.openfinance.personalfinancialdata.domain.port.out;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AisCachePort {

    Optional<List<AccountSnapshot>> getAccounts(String key, Instant now);

    void putAccounts(String key, List<AccountSnapshot> accounts, Instant expiresAt);

    Optional<List<BalanceSnapshot>> getBalances(String key, Instant now);

    void putBalances(String key, List<BalanceSnapshot> balances, Instant expiresAt);
}
