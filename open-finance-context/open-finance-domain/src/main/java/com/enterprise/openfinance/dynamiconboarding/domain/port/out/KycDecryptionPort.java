package com.enterprise.openfinance.dynamiconboarding.domain.port.out;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;

public interface KycDecryptionPort {

    OnboardingApplicantProfile decrypt(String encryptedPayload, String interactionId);
}
