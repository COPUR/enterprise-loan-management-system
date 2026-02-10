package com.enterprise.openfinance.businessfinancialdata.domain.port.in;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.businessfinancialdata.domain.query.GetCorporateBalancesQuery;
import com.enterprise.openfinance.businessfinancialdata.domain.query.GetCorporateTransactionsQuery;
import com.enterprise.openfinance.businessfinancialdata.domain.query.ListCorporateAccountsQuery;

public interface CorporateTreasuryUseCase {

    CorporateAccountListResult listAccounts(ListCorporateAccountsQuery query);

    CorporateBalanceListResult getBalances(GetCorporateBalancesQuery query);

    CorporatePagedResult<CorporateTransactionSnapshot> getTransactions(GetCorporateTransactionsQuery query);
}
