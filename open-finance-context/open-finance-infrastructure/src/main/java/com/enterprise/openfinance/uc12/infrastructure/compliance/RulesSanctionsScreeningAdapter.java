package com.enterprise.openfinance.uc12.infrastructure.compliance;

import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.uc12.domain.port.out.SanctionsScreeningPort;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class RulesSanctionsScreeningAdapter implements SanctionsScreeningPort {

    private final Set<String> blockedNames;

    public RulesSanctionsScreeningAdapter(Set<String> blockedNames) {
        this.blockedNames = blockedNames.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isBlocked(OnboardingApplicantProfile profile, String interactionId) {
        String normalized = profile.fullName().trim().toUpperCase(Locale.ROOT);
        return blockedNames.contains(normalized);
    }
}
