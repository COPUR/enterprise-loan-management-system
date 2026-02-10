package com.enterprise.openfinance.uc12.infrastructure.event;

import com.enterprise.openfinance.uc12.domain.model.OnboardingAccount;
import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.uc12.domain.port.out.OnboardingEventPort;
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
