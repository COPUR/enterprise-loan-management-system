package com.enterprise.openfinance.uc02.domain.port.out;

import com.enterprise.openfinance.uc02.domain.model.AccountSnapshot;

import java.util.List;
import java.util.Optional;

public interface AccountReadPort {

    List<AccountSnapshot> findByPsuId(String psuId);

    Optional<AccountSnapshot> findById(String accountId);
}
