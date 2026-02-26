package com.enterprise.openfinance.consent.domain.port.out;

import com.enterprise.openfinance.consent.domain.model.Consent;

import java.util.List;
import java.util.Optional;

public interface ConsentStore {

    Consent save(Consent consent);

    Optional<Consent> findById(String consentId);

    List<Consent> findByCustomerId(String customerId);
}
