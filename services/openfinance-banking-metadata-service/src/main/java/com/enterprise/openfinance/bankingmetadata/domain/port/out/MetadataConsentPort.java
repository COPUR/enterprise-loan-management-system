package com.enterprise.openfinance.bankingmetadata.domain.port.out;

import com.enterprise.openfinance.bankingmetadata.domain.model.MetadataConsentContext;

import java.util.Optional;

public interface MetadataConsentPort {

    Optional<MetadataConsentContext> findById(String consentId);
}
