package com.enterprise.openfinance.corporatetreasury.domain.port.out;

import com.enterprise.openfinance.corporatetreasury.domain.model.CorporateConsentContext;

import java.util.Optional;

public interface CorporateConsentPort {

    Optional<CorporateConsentContext> findById(String consentId);
}
