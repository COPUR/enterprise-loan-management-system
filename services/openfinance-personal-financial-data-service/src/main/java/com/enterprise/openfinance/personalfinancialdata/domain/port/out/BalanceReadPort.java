package com.enterprise.openfinance.personalfinancialdata.domain.port.out;

import com.enterprise.openfinance.personalfinancialdata.domain.model.BalanceSnapshot;

import java.util.List;

public interface BalanceReadPort {

    List<BalanceSnapshot> findByAccountId(String accountId);
}
