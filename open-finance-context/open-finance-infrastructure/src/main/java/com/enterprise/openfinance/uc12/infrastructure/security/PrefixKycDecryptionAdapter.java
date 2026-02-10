package com.enterprise.openfinance.uc12.infrastructure.security;

import com.enterprise.openfinance.uc12.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.uc12.domain.port.out.KycDecryptionPort;
import org.springframework.stereotype.Component;

@Component
public class PrefixKycDecryptionAdapter implements KycDecryptionPort {

    @Override
    public OnboardingApplicantProfile decrypt(String encryptedPayload, String interactionId) {
        if (encryptedPayload == null || !encryptedPayload.startsWith("jwe:")) {
            throw new IllegalArgumentException("Decryption Failed");
        }

        String rawPayload = encryptedPayload.substring(4);
        String[] tokens = rawPayload.split("\\|");
        if (tokens.length < 3) {
            throw new IllegalArgumentException("Decryption Failed");
        }

        return new OnboardingApplicantProfile(tokens[0], tokens[1], tokens[2]);
    }
}
