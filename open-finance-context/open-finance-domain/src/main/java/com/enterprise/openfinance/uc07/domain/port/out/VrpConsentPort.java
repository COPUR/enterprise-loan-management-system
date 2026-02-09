package com.enterprise.openfinance.uc07.domain.port.out;

import com.enterprise.openfinance.uc07.domain.model.VrpConsent;

import java.util.Optional;

public interface VrpConsentPort {

    VrpConsent save(VrpConsent consent);

    Optional<VrpConsent> findById(String consentId);
}
