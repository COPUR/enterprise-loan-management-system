package com.enterprise.openfinance.uc05.domain.port.in;

import com.enterprise.openfinance.uc05.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.uc05.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.uc05.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.uc05.domain.query.GetCorporateBalancesQuery;
import com.enterprise.openfinance.uc05.domain.query.GetCorporateTransactionsQuery;
import com.enterprise.openfinance.uc05.domain.query.ListCorporateAccountsQuery;

public interface CorporateTreasuryUseCase {

    CorporateAccountListResult listAccounts(ListCorporateAccountsQuery query);

    CorporateBalanceListResult getBalances(GetCorporateBalancesQuery query);

    CorporatePagedResult<CorporateTransactionSnapshot> getTransactions(GetCorporateTransactionsQuery query);
}
