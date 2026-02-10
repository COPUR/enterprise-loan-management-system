package com.enterprise.openfinance.uc13.domain.port.out;

import com.enterprise.openfinance.uc13.domain.model.PayRequest;

import java.util.Optional;

public interface PayRequestRepositoryPort {

    PayRequest save(PayRequest request);

    Optional<PayRequest> findById(String consentId);
}
