package com.enterprise.openfinance.businessfinancialdata.domain.port.out;

import com.enterprise.openfinance.businessfinancialdata.domain.model.CorporateConsentContext;

import java.util.Optional;

public interface CorporateConsentPort {

    Optional<CorporateConsentContext> findById(String consentId);
}
