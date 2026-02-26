package com.enterprise.openfinance.corporatetreasury.domain.port.out;

import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateBalanceSnapshot;

import java.util.List;

public interface CorporateBalanceReadPort {

    List<CorporateBalanceSnapshot> findByMasterAccountId(String masterAccountId);
}
