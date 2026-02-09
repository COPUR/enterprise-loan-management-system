package com.enterprise.openfinance.uc05.domain.port.out;

import com.enterprise.openfinance.uc05.domain.model.CorporateBalanceSnapshot;

import java.util.List;

public interface CorporateBalanceReadPort {

    List<CorporateBalanceSnapshot> findByMasterAccountId(String masterAccountId);
}
