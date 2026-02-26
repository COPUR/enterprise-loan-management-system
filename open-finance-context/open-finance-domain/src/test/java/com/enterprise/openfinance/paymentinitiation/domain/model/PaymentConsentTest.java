package com.enterprise.openfinance.paymentinitiation.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentConsentTest {

    private static final Instant NOW = Instant.parse("2026-02-09T10:00:00Z");

    @Test
    void shouldAllowInitiationWhenConsentMatches() {
        PaymentConsent consent = new PaymentConsent(
                "CONS-001",
                PaymentConsentStatus.AUTHORIZED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-12-31T00:00:00Z")
        );

        PaymentInitiation initiation = new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        );

        assertThat(consent.canInitiate(initiation, NOW)).isTrue();
    }

    @Test
    void shouldRejectInitiationWhenConsentDoesNotMatch() {
        PaymentConsent consent = new PaymentConsent(
                "CONS-001",
                PaymentConsentStatus.AUTHORIZED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-12-31T00:00:00Z")
        );

        PaymentInitiation amountTooHigh = new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("600.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        );
        PaymentInitiation wrongCurrency = new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "USD",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        );
        PaymentInitiation wrongPayee = new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000999999999",
                "Vendor LLC",
                null
        );

        assertThat(consent.canInitiate(amountTooHigh, NOW)).isFalse();
        assertThat(consent.canInitiate(wrongCurrency, NOW)).isFalse();
        assertThat(consent.canInitiate(wrongPayee, NOW)).isFalse();
    }

    @Test
    void shouldRejectInitiationForExpiredOrNonAuthorizedConsent() {
        PaymentConsent expired = new PaymentConsent(
                "CONS-001",
                PaymentConsentStatus.AUTHORIZED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-01-01T00:00:00Z")
        );
        PaymentConsent revoked = new PaymentConsent(
                "CONS-001",
                PaymentConsentStatus.REVOKED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-12-31T00:00:00Z")
        );

        PaymentInitiation initiation = new PaymentInitiation(
                "INSTR-001",
                "E2E-001",
                "ACC-DEBTOR-001",
                new BigDecimal("100.00"),
                "AED",
                "IBAN",
                "AE120001000000123456789",
                "Vendor LLC",
                null
        );

        assertThat(expired.canInitiate(initiation, NOW)).isFalse();
        assertThat(revoked.canInitiate(initiation, NOW)).isFalse();
    }

    @Test
    void shouldRejectInvalidConsentState() {
        assertThatThrownBy(() -> new PaymentConsent(
                " ",
                PaymentConsentStatus.AUTHORIZED,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-12-31T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("consentId is required");

        assertThatThrownBy(() -> new PaymentConsent(
                "CONS-001",
                null,
                new BigDecimal("500.00"),
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-12-31T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("status is required");

        assertThatThrownBy(() -> new PaymentConsent(
                "CONS-001",
                PaymentConsentStatus.AUTHORIZED,
                BigDecimal.ZERO,
                "AED",
                PaymentConsent.hashPayee("AE120001000000123456789"),
                Instant.parse("2026-12-31T00:00:00Z")
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("maxAmount must be greater than zero");

        assertThatThrownBy(() -> PaymentConsent.hashPayee(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creditorAccountIdentification is required");
    }
}
