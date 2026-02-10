package com.enterprise.openfinance.uc12.domain.port.out;

import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;

public interface KycDecryptionPort {

    OnboardingApplicantProfile decrypt(String encryptedPayload, String interactionId);
}
