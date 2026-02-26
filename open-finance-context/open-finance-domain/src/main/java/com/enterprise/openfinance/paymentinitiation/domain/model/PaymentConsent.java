package com.enterprise.openfinance.paymentinitiation.domain.model;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

public record PaymentConsent(
        String consentId,
        PaymentConsentStatus status,
        BigDecimal maxAmount,
        String currency,
        String payeeHash,
        Instant expiryAt
) {
    public PaymentConsent {
        if (isBlank(consentId)) {
            throw new IllegalArgumentException("consentId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (maxAmount == null || maxAmount.signum() <= 0) {
            throw new IllegalArgumentException("maxAmount must be greater than zero");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (isBlank(payeeHash)) {
            throw new IllegalArgumentException("payeeHash is required");
        }
        if (expiryAt == null) {
            throw new IllegalArgumentException("expiryAt is required");
        }

        consentId = consentId.trim();
        maxAmount = maxAmount.stripTrailingZeros();
        currency = currency.trim().toUpperCase(Locale.ROOT);
        payeeHash = payeeHash.trim().toLowerCase(Locale.ROOT);
    }

    public boolean canInitiate(PaymentInitiation initiation, Instant now) {
        if (status != PaymentConsentStatus.AUTHORIZED) {
            return false;
        }
        if (!expiryAt.isAfter(now)) {
            return false;
        }
        if (!currency.equalsIgnoreCase(initiation.currency())) {
            return false;
        }
        if (initiation.instructedAmount().compareTo(maxAmount) > 0) {
            return false;
        }
        return payeeHash.equals(hashPayee(initiation.creditorAccountIdentification()));
    }

    public static String hashPayee(String creditorAccountIdentification) {
        if (isBlank(creditorAccountIdentification)) {
            throw new IllegalArgumentException("creditorAccountIdentification is required");
        }
        String normalized = creditorAccountIdentification.trim().replace(" ", "").toUpperCase(Locale.ROOT);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
