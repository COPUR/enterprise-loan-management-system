package com.enterprise.openfinance.dynamiconboarding.domain.model;

public record OnboardingApplicantProfile(
        String fullName,
        String nationalId,
        String countryCode
) {

    public OnboardingApplicantProfile {
        if (isBlank(fullName)) {
            throw new IllegalArgumentException("fullName is required");
        }
        if (isBlank(nationalId)) {
            throw new IllegalArgumentException("nationalId is required");
        }
        if (!isCountryCode(countryCode)) {
            throw new IllegalArgumentException("countryCode must be alpha-2");
        }

        fullName = fullName.trim();
        nationalId = nationalId.trim();
        countryCode = countryCode.trim().toUpperCase();
    }

    private static boolean isCountryCode(String value) {
        return value != null && value.trim().matches("[A-Za-z]{2}");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
