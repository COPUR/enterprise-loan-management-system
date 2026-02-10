package com.enterprise.openfinance.personalfinancialdata.domain.port.out;

import com.enterprise.openfinance.personalfinancialdata.domain.model.AisConsentContext;

import java.util.Optional;

public interface AisConsentPort {

    Optional<AisConsentContext> findById(String consentId);
}
