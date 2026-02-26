package com.enterprise.openfinance.insurancedata.domain.port.out;

import com.enterprise.openfinance.insurancedata.domain.model.InsuranceConsentContext;

import java.util.Optional;

public interface InsuranceConsentPort {

    Optional<InsuranceConsentContext> findById(String consentId);
}
