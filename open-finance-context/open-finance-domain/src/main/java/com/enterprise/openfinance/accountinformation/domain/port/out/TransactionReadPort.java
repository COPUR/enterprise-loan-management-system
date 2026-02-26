package com.enterprise.openfinance.accountinformation.domain.port.out;

import com.enterprise.openfinance.accountinformation.domain.model.TransactionSnapshot;

import java.util.List;

public interface TransactionReadPort {

    List<TransactionSnapshot> findByAccountId(String accountId);
}
