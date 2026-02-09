package com.enterprise.openfinance.uc08.domain.port.out;

import com.enterprise.openfinance.uc08.domain.model.BulkConsentContext;

import java.util.Optional;

public interface BulkConsentPort {

    Optional<BulkConsentContext> findById(String consentId);
}
