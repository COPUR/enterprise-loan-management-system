package com.enterprise.openfinance.uc06.domain.port.out;

import com.enterprise.openfinance.uc06.domain.model.PaymentConsent;

import java.util.Optional;

public interface PaymentConsentPort {
    Optional<PaymentConsent> findById(String consentId);
}
