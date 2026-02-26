package com.enterprise.openfinance.dynamiconboarding.domain.port.out;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;

public interface SanctionsScreeningPort {

    boolean isBlocked(OnboardingApplicantProfile profile, String interactionId);
}
