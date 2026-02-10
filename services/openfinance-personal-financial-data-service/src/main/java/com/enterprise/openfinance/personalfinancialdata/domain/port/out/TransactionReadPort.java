package com.enterprise.openfinance.personalfinancialdata.domain.port.out;

import com.enterprise.openfinance.personalfinancialdata.domain.model.TransactionSnapshot;

import java.util.List;

public interface TransactionReadPort {

    List<TransactionSnapshot> findByAccountId(String accountId);
}
