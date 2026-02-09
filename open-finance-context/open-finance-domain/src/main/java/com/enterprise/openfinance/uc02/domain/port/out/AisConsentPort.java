package com.enterprise.openfinance.uc02.domain.port.out;

import com.enterprise.openfinance.uc02.domain.model.AisConsentContext;

import java.util.Optional;

public interface AisConsentPort {

    Optional<AisConsentContext> findById(String consentId);
}
