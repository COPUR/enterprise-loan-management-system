package com.enterprise.openfinance.dynamiconboarding.domain.port.out;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;

public interface OnboardingEventPort {

    void publishAccountOpened(OnboardingAccount account);

    void publishOnboardingRejected(OnboardingApplicantProfile profile, String tppId, String reason);
}
