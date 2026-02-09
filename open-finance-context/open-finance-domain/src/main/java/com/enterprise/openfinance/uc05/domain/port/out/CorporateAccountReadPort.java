package com.enterprise.openfinance.uc05.domain.port.out;

import com.enterprise.openfinance.uc05.domain.model.CorporateAccountSnapshot;

import java.util.List;
import java.util.Optional;

public interface CorporateAccountReadPort {

    List<CorporateAccountSnapshot> findByCorporateId(String corporateId);

    Optional<CorporateAccountSnapshot> findById(String accountId);
}
