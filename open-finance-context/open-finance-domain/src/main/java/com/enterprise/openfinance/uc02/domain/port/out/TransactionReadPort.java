package com.enterprise.openfinance.uc02.domain.port.out;

import com.enterprise.openfinance.uc02.domain.model.TransactionSnapshot;

import java.util.List;

public interface TransactionReadPort {

    List<TransactionSnapshot> findByAccountId(String accountId);
}
