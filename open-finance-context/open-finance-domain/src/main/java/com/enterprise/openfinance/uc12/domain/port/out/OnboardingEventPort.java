package com.enterprise.openfinance.uc12.domain.port.out;

import com.enterprise.openfinance.uc12.domain.model.OnboardingAccount;
import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;

public interface OnboardingEventPort {

    void publishAccountOpened(OnboardingAccount account);

    void publishOnboardingRejected(OnboardingApplicantProfile profile, String tppId, String reason);
}
