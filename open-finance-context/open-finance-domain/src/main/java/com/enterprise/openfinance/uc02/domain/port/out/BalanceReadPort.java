package com.enterprise.openfinance.uc02.domain.port.out;

import com.enterprise.openfinance.uc02.domain.model.BalanceSnapshot;

import java.util.List;

public interface BalanceReadPort {

    List<BalanceSnapshot> findByAccountId(String accountId);
}
