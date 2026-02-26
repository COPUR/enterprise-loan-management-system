package com.enterprise.openfinance.accountinformation.domain.port.in;

import com.enterprise.openfinance.accountinformation.domain.model.AccountListResult;
import com.enterprise.openfinance.accountinformation.domain.model.AccountSnapshot;
import com.enterprise.openfinance.accountinformation.domain.model.BalanceListResult;
import com.enterprise.openfinance.accountinformation.domain.model.PagedResult;
import com.enterprise.openfinance.accountinformation.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.accountinformation.domain.query.GetAccountQuery;
import com.enterprise.openfinance.accountinformation.domain.query.GetBalancesQuery;
import com.enterprise.openfinance.accountinformation.domain.query.GetTransactionsQuery;
import com.enterprise.openfinance.accountinformation.domain.query.ListAccountsQuery;

public interface AccountInformationUseCase {

    AccountListResult listAccounts(ListAccountsQuery query);

    AccountSnapshot getAccount(GetAccountQuery query);

    BalanceListResult getBalances(GetBalancesQuery query);

    PagedResult<TransactionSnapshot> getTransactions(GetTransactionsQuery query);
}
