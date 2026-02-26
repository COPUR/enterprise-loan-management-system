package com.enterprise.openfinance.paymentinitiation.domain.port.out;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentConsent;

import java.util.Optional;

public interface PaymentConsentPort {
    Optional<PaymentConsent> findById(String consentId);
}
