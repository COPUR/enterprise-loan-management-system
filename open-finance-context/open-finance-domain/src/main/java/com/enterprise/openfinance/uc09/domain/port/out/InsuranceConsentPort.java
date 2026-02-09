package com.enterprise.openfinance.uc09.domain.port.out;

import com.enterprise.openfinance.uc09.domain.model.InsuranceConsentContext;

import java.util.Optional;

public interface InsuranceConsentPort {

    Optional<InsuranceConsentContext> findById(String consentId);
}
