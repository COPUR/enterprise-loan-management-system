package com.enterprise.openfinance.uc05.domain.port.out;

import com.enterprise.openfinance.uc05.domain.model.CorporateConsentContext;

import java.util.Optional;

public interface CorporateConsentPort {

    Optional<CorporateConsentContext> findById(String consentId);
}
