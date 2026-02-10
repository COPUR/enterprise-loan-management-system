package com.enterprise.openfinance.uc12.domain.port.out;

import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;

public interface SanctionsScreeningPort {

    boolean isBlocked(OnboardingApplicantProfile profile, String interactionId);
}
