package com.enterprise.openfinance.uc02.domain.port.in;

import com.enterprise.openfinance.uc02.domain.model.AccountListResult;
import com.enterprise.openfinance.uc02.domain.model.AccountSnapshot;
import com.enterprise.openfinance.uc02.domain.model.BalanceListResult;
import com.enterprise.openfinance.uc02.domain.model.PagedResult;
import com.enterprise.openfinance.uc02.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.uc02.domain.query.GetAccountQuery;
import com.enterprise.openfinance.uc02.domain.query.GetBalancesQuery;
import com.enterprise.openfinance.uc02.domain.query.GetTransactionsQuery;
import com.enterprise.openfinance.uc02.domain.query.ListAccountsQuery;

public interface AccountInformationUseCase {

    AccountListResult listAccounts(ListAccountsQuery query);

    AccountSnapshot getAccount(GetAccountQuery query);

    BalanceListResult getBalances(GetBalancesQuery query);

    PagedResult<TransactionSnapshot> getTransactions(GetTransactionsQuery query);
}
