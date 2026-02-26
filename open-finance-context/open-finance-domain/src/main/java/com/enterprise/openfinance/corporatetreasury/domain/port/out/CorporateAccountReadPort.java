package com.enterprise.openfinance.corporatetreasury.domain.port.out;

import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateAccountSnapshot;

import java.util.List;
import java.util.Optional;

public interface CorporateAccountReadPort {

    List<CorporateAccountSnapshot> findByCorporateId(String corporateId);

    Optional<CorporateAccountSnapshot> findById(String accountId);
}
