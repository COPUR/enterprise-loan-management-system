package com.enterprise.openfinance.paymentinitiation.infrastructure.persistence;

import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentConsent;
import com.enterprise.openfinance.paymentinitiation.domain.model.PaymentConsentStatus;
import com.enterprise.openfinance.paymentinitiation.domain.port.out.PaymentConsentPort;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentConsentAdapter implements PaymentConsentPort {

    private final Map<String, PaymentConsent> data = new ConcurrentHashMap<>();

    public InMemoryPaymentConsentAdapter() {
        seedDefaults();
    }

    @Override
    public Optional<PaymentConsent> findById(String consentId) {
        return Optional.ofNullable(data.get(consentId));
    }

    private void seedDefaults() {
        data.put("CONS-001", new PaymentConsent(
                "CONS-001",
                PaymentConsentStatus.AUTHORIZED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2030-12-31T00:00:00Z")
        ));
        data.put("CONS-EXPIRED", new PaymentConsent(
                "CONS-EXPIRED",
                PaymentConsentStatus.AUTHORIZED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2020-12-31T00:00:00Z")
        ));
    }
}
