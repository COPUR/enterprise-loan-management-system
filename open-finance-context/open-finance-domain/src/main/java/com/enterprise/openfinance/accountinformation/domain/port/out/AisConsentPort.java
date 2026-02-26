package com.enterprise.openfinance.accountinformation.domain.port.out;

import com.enterprise.openfinance.accountinformation.domain.model.AisConsentContext;

import java.util.Optional;

public interface AisConsentPort {

    Optional<AisConsentContext> findById(String consentId);
}
