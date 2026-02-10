package com.enterprise.openfinance.businessfinancialdata.domain.port.out;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateTransactionSnapshot;

import java.util.List;
import java.util.Set;

public interface CorporateTransactionReadPort {

    List<CorporateTransactionSnapshot> findByAccountIds(Set<String> accountIds);
}
