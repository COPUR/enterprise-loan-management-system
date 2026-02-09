package com.enterprise.openfinance.uc09.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record MotorPolicy(
        String policyId,
        String policyNumber,
        String holderName,
        String holderNameMasked,
        String vehicleMake,
        String vehicleModel,
        int vehicleYear,
        BigDecimal premiumAmount,
        String currency,
        LocalDate startDate,
        LocalDate endDate,
        MotorPolicyStatus status,
        List<String> coverages
) {

    public MotorPolicy {
        if (isBlank(policyId)) {
            throw new IllegalArgumentException("policyId is required");
        }
        if (isBlank(policyNumber)) {
            throw new IllegalArgumentException("policyNumber is required");
        }
        if (isBlank(holderName)) {
            throw new IllegalArgumentException("holderName is required");
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
        if (premiumAmount == null || premiumAmount.signum() <= 0) {
            throw new IllegalArgumentException("premiumAmount must be positive");
        }
        if (isBlank(currency)) {
            throw new IllegalArgumentException("currency is required");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("startDate is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("endDate is required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate cannot be before startDate");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }

        List<String> normalizedCoverages = new ArrayList<>();
        for (String coverage : coverages == null ? List.<String>of() : coverages) {
            if (isBlank(coverage)) {
                throw new IllegalArgumentException("coverage cannot be blank");
            }
            normalizedCoverages.add(coverage.trim());
        }
        if (normalizedCoverages.isEmpty()) {
            throw new IllegalArgumentException("coverages is required");
        }

        policyId = policyId.trim();
        policyNumber = policyNumber.trim();
        holderName = holderName.trim();
        holderNameMasked = isBlank(holderNameMasked) ? mask(holderName) : holderNameMasked.trim();
        vehicleMake = vehicleMake.trim();
        vehicleModel = vehicleModel.trim();
        currency = currency.trim();
        coverages = List.copyOf(normalizedCoverages);
    }

    public boolean isActive() {
        return status.isActive();
    }

    private static String mask(String value) {
        String trimmed = value.trim();
        if (trimmed.length() <= 1) {
            return "*";
        }
        return trimmed.charAt(0) + "***";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
