package com.enterprise.openfinance.uc12.domain.model;

import java.time.Instant;

public record OnboardingAccount(
        String accountId,
        String tppId,
        String customerId,
        OnboardingApplicantProfile applicantProfile,
        String primaryCurrency,
        OnboardingAccountStatus status,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {

    public OnboardingAccount {
        if (isBlank(accountId)) {
            throw new IllegalArgumentException("accountId is required");
        }
        if (isBlank(tppId)) {
            throw new IllegalArgumentException("tppId is required");
        }
        if (isBlank(customerId)) {
            throw new IllegalArgumentException("customerId is required");
        }
        if (applicantProfile == null) {
            throw new IllegalArgumentException("applicantProfile is required");
        }
        if (!isCurrency(primaryCurrency)) {
            throw new IllegalArgumentException("primaryCurrency must be ISO-4217 alpha-3");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException("timestamps are required");
        }
        if (status == OnboardingAccountStatus.REJECTED && isBlank(rejectionReason)) {
            throw new IllegalArgumentException("rejectionReason is required when status is REJECTED");
        }

        accountId = accountId.trim();
        tppId = tppId.trim();
        customerId = customerId.trim();
        primaryCurrency = primaryCurrency.trim().toUpperCase();
        rejectionReason = rejectionReason == null ? null : rejectionReason.trim();
    }

    public OnboardingAccount reject(String reason, Instant now) {
        if (status == OnboardingAccountStatus.REJECTED) {
            return this;
        }
        if (isBlank(reason)) {
            throw new IllegalArgumentException("reason is required");
        }
        return new OnboardingAccount(
                accountId,
                tppId,
                customerId,
                applicantProfile,
                primaryCurrency,
                OnboardingAccountStatus.REJECTED,
                reason,
                createdAt,
                now
        );
    }

    public boolean belongsTo(String candidateTppId) {
        return tppId.equals(candidateTppId);
    }

    private static boolean isCurrency(String value) {
        return value != null && value.trim().matches("[A-Za-z]{3}");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
