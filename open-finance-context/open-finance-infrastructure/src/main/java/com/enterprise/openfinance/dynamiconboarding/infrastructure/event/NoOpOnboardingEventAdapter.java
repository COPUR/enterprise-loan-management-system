package com.enterprise.openfinance.dynamiconboarding.infrastructure.event;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingAccount;
import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.OnboardingEventPort;
import org.springframework.stereotype.Component;

@Component
public class NoOpOnboardingEventAdapter implements OnboardingEventPort {

    @Override
    public void publishAccountOpened(OnboardingAccount account) {
        // Intentionally no-op for baseline implementation.
    }

    @Override
    public void publishOnboardingRejected(OnboardingApplicantProfile profile, String tppId, String reason) {
        // Intentionally no-op for baseline implementation.
    }
}
