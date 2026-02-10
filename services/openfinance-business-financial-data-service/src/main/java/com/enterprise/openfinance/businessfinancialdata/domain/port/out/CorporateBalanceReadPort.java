package com.enterprise.openfinance.businessfinancialdata.domain.port.out;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateBalanceSnapshot;

import java.util.List;

public interface CorporateBalanceReadPort {

    List<CorporateBalanceSnapshot> findByMasterAccountId(String masterAccountId);
}
