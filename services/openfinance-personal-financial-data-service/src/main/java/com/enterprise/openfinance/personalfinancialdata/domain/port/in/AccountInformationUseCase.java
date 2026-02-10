package com.enterprise.openfinance.personalfinancialdata.domain.port.in;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountListResult;
import com.enterprise.openfinance.personalfinancialdata.domain.model.AccountSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceListResult;
import com.enterprise.openfinance.personalfinancialdata.domain.model.PagedResult;
import com.enterprise.openfinance.personalfinancialdata.domain.model.TransactionSnapshot;
import com.enterprise.openfinance.personalfinancialdata.domain.query.GetAccountQuery;
import com.enterprise.openfinance.personalfinancialdata.domain.query.GetBalancesQuery;
import com.enterprise.openfinance.personalfinancialdata.domain.query.GetTransactionsQuery;
import com.enterprise.openfinance.personalfinancialdata.domain.query.ListAccountsQuery;

public interface AccountInformationUseCase {

    AccountListResult listAccounts(ListAccountsQuery query);

    AccountSnapshot getAccount(GetAccountQuery query);

    BalanceListResult getBalances(GetBalancesQuery query);

    PagedResult<TransactionSnapshot> getTransactions(GetTransactionsQuery query);
}
