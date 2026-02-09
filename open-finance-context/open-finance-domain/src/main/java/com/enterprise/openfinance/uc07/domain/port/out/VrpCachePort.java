package com.enterprise.openfinance.uc07.domain.port.out;

import com.enterprise.openfinance.uc07.domain.model.VrpConsent;
import com.enterprise.openfinance.uc07.domain.model.VrpPayment;

import java.time.Instant;
import java.util.Optional;

public interface VrpCachePort {

    Optional<VrpConsent> getConsent(String key, Instant now);

    void putConsent(String key, VrpConsent consent, Instant expiresAt);

    Optional<VrpPayment> getPayment(String key, Instant now);

    void putPayment(String key, VrpPayment payment, Instant expiresAt);
}
