package com.enterprise.openfinance.uc05.domain.port.out;

import com.enterprise.openfinance.uc05.domain.model.CorporateTransactionSnapshot;

import java.util.List;
import java.util.Set;

public interface CorporateTransactionReadPort {

    List<CorporateTransactionSnapshot> findByAccountIds(Set<String> accountIds);
}
