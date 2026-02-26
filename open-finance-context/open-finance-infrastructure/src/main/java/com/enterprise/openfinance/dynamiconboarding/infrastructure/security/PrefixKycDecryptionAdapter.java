package com.enterprise.openfinance.dynamiconboarding.infrastructure.security;

import com.enterprise.openfinance.dynamiconboarding.domain.model.OnboardingApplicantProfile;
import com.enterprise.openfinance.dynamiconboarding.domain.port.out.KycDecryptionPort;
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
