package com.enterprise.openfinance.corporatetreasury.domain.port.in;

import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateAccountListResult;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateBalanceListResult;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporatePagedResult;
import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateTransactionSnapshot;
import com.enterprise.openfinance.corporatetreasury.domain.query.GetCorporateBalancesQuery;
import com.enterprise.openfinance.corporatetreasury.domain.query.GetCorporateTransactionsQuery;
import com.enterprise.openfinance.corporatetreasury.domain.query.ListCorporateAccountsQuery;

public interface CorporateTreasuryUseCase {

    CorporateAccountListResult listAccounts(ListCorporateAccountsQuery query);

    CorporateBalanceListResult getBalances(GetCorporateBalancesQuery query);

    CorporatePagedResult<CorporateTransactionSnapshot> getTransactions(GetCorporateTransactionsQuery query);
}
