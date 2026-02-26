package com.enterprise.openfinance.accountinformation.domain.port.out;

import com.enterprise.openfinance.accountinformation.domain.model.BalanceSnapshot;

import java.util.List;

public interface BalanceReadPort {

    List<BalanceSnapshot> findByAccountId(String accountId);
}
