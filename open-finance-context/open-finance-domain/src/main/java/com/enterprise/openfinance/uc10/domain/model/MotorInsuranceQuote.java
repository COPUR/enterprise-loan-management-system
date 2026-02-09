package com.enterprise.openfinance.uc10.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MotorInsuranceQuote(
        String quoteId,
        String tppId,
        String vehicleMake,
        String vehicleModel,
        int vehicleYear,
        int driverAge,
        int licenseDurationYears,
        BigDecimal premiumAmount,
        String currency,
        QuoteStatus status,
        Instant validUntil,
        String riskHash,
        String policyId,
        String policyNumber,
        String certificateId,
        String paymentReference,
        Instant createdAt,
        Instant updatedAt
) {

    public MotorInsuranceQuote {
        if (isBlank(quoteId)) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(vehicleMake)) {
            throw new IllegalArgumentException("vehicleMake is required");
        }
        if (isBlank(vehicleModel)) {
            throw new IllegalArgumentException("vehicleModel is required");
        }
        if (vehicleYear < 1900 || vehicleYear > 2100) {
            throw new IllegalArgumentException("vehicleYear is invalid");
        }
        if (driverAge < 18 || driverAge > 99) {
            throw new IllegalArgumentException("driverAge is invalid");
        }
        if (licenseDurationYears < 0 || licenseDurationYears > 80) {
            throw new IllegalArgumentException("licenseDurationYears is invalid");
        }
        if (premiumAmount == null || premiumAmount.signum() <= 0) {
            throw new IllegalArgumentException("premiumAmount must be positive");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (validUntil == null) {
            throw new IllegalArgumentException("validUntil is required");
        }
        if (isBlank(riskHash)) {
            throw new IllegalArgumentException("riskHash is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("timestamps are required");
        }

        quoteId = quoteId.trim();
        tppId = tppId.trim();
        vehicleMake = vehicleMake.trim().toUpperCase();
        vehicleModel = vehicleModel.trim().toUpperCase();
        currency = currency.trim().toUpperCase();
        riskHash = riskHash.trim();
        policyId = normalizeOptional(policyId);
        policyNumber = normalizeOptional(policyNumber);
        certificateId = normalizeOptional(certificateId);
        paymentReference = normalizeOptional(paymentReference);
    }

    public boolean isExpired(Instant now) {
        return !validUntil.isAfter(now);
    }

    public boolean belongsTo(String candidateTppId) {
        return tppId.equals(candidateTppId);
    }

    public MotorInsuranceQuote accept(String nextPolicyId,
                                      String nextPolicyNumber,
                                      String nextCertificateId,
                                      String nextPaymentReference,
                                      Instant now) {
        return new MotorInsuranceQuote(
                quoteId,
                tppId,
                vehicleMake,
                vehicleModel,
                vehicleYear,
                driverAge,
                licenseDurationYears,
                premiumAmount,
                currency,
                QuoteStatus.ACCEPTED,
                validUntil,
                riskHash,
                nextPolicyId,
                nextPolicyNumber,
                nextCertificateId,
                nextPaymentReference,
                createdAt,
                now
        );
    }

    public MotorInsuranceQuote expire(Instant now) {
        if (status == QuoteStatus.EXPIRED) {
            return this;
        }
        return new MotorInsuranceQuote(
                quoteId,
                tppId,
                vehicleMake,
                vehicleModel,
                vehicleYear,
                driverAge,
                licenseDurationYears,
                premiumAmount,
                currency,
                QuoteStatus.EXPIRED,
                validUntil,
                riskHash,
                policyId,
                policyNumber,
                certificateId,
                paymentReference,
                createdAt,
                now
        );
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
