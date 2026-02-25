package com.enterprise.openfinance.consentauthorization.application;

import com.enterprise.openfinance.consentauthorization.domain.port.out.PkceHashPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class PkceServiceTest {

    private final PkceHashPort sha256Port = payload -> {
        try {
            return MessageDigest.getInstance("SHA-256").digest(payload);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    };

    @Test
    void shouldGenerateVerifierWithinRfcLengthBounds() {
        PkceService service = new PkceService(sha256Port, new SecureRandom());

        String verifier = service.generateCodeVerifier();

        assertThat(verifier.length()).isBetween(43, 128);
        assertThat(verifier).matches("^[A-Za-z0-9\\-._~]+$");
    }

    @Test
    void shouldGenerateChallengeUsingRfc7636Vector() {
        PkceService service = new PkceService(sha256Port, new SecureRandom());
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        String challenge = service.deriveS256CodeChallenge(verifier);

        assertThat(challenge).isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
        assertThat(service.verifyCodeChallenge(verifier, challenge)).isTrue();
    }

    @Test
    void shouldRejectInvalidVerifierCharset() {
        PkceService service = new PkceService(sha256Port, new SecureRandom());

        assertThatThrownBy(() -> service.deriveS256CodeChallenge("invalid verifier with spaces"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("code_verifier");
    }

    @Test
    void shouldRejectVerifierShorterThanRfcMinimum() {
        PkceService service = new PkceService(sha256Port, new SecureRandom());
        String shortVerifier = "abc123456789012345678901234567890123456789";

        assertThat(shortVerifier.getBytes(StandardCharsets.US_ASCII)).hasSize(42);
        assertThatThrownBy(() -> service.deriveS256CodeChallenge(shortVerifier))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
