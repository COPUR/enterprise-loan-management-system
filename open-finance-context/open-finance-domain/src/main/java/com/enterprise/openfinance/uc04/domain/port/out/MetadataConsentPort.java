package com.enterprise.openfinance.uc04.domain.port.out;

import com.enterprise.openfinance.uc04.domain.model.MetadataConsentContext;

import java.util.Optional;

public interface MetadataConsentPort {

    Optional<MetadataConsentContext> findById(String consentId);
}
