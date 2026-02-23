package com.enterprise.openfinance.consentauthorization.application;

import com.enterprise.openfinance.consentauthorization.domain.port.out.PkceHashPort;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
public class PkceService {

    private static final Pattern CODE_VERIFIER_PATTERN = Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");
    private static final int DEFAULT_VERIFIER_BYTES = 64;

    private final PkceHashPort pkceHashPort;
    private final SecureRandom secureRandom;

    public PkceService(PkceHashPort pkceHashPort, SecureRandom secureRandom) {
        this.pkceHashPort = pkceHashPort;
        this.secureRandom = secureRandom;
    }

    public String generateCodeVerifier() {
        byte[] bytes = new byte[DEFAULT_VERIFIER_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String deriveS256CodeChallenge(String codeVerifier) {
        validateCodeVerifier(codeVerifier);
        byte[] digest = pkceHashPort.sha256(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    public boolean verifyCodeChallenge(String codeVerifier, String expectedCodeChallenge) {
        if (expectedCodeChallenge == null || expectedCodeChallenge.isBlank()) {
            return false;
        }
        String generated = deriveS256CodeChallenge(codeVerifier);
        return MessageDigest.isEqual(
                generated.getBytes(StandardCharsets.US_ASCII),
                expectedCodeChallenge.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private static void validateCodeVerifier(String codeVerifier) {
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("code_verifier is required");
        }
        if (!CODE_VERIFIER_PATTERN.matcher(codeVerifier).matches()) {
            throw new IllegalArgumentException("code_verifier must be 43-128 chars using RFC7636 unreserved charset");
        }
    }
}

