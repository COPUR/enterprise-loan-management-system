package com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.port.out;

import com.enterprise.openfinance.consentauthorization.jwtlifecycle.domain.model.InternalSystemSecretRecord;

import java.util.Optional;

public interface InternalSystemSecretPort {

    Optional<InternalSystemSecretRecord> findBySecretKey(String secretKey);

    InternalSystemSecretRecord save(InternalSystemSecretRecord record);
}
