package com.bank.loan.loan.security.dpop;

import com.bank.loan.loan.security.dpop.model.DPoPProof;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
import com.bank.loan.loan.security.dpop.util.DPoPTestKeyGenerator;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DPoP Proof Validation Service Tests")
class DPoPProofValidationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private DPoPProofValidationService dpopProofValidationService;

    private ECKey testKeyPair;
    private String validDPoPProof;
    private DPoPTestKeyGenerator keyGenerator;

    @BeforeEach
    void setUp() throws Exception {
        keyGenerator = new DPoPTestKeyGenerator();
        testKeyPair = keyGenerator.generateECKey();
        validDPoPProof = keyGenerator.createValidDPoPProof(testKeyPair, "POST", "https://auth.example.com/token");
    }

    @Nested
    @DisplayName("Valid DPoP Proof Tests")
    class ValidDPoPProofTests {

        @Test
        @DisplayName("Should validate correct DPoP proof successfully")
        void shouldValidateCorrectDPoPProof() {
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

            assertThatCode(() -> dpopProofValidationService.validateDPoPProof(
                    validDPoPProof, "POST", "https://auth.example.com/token", null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should validate DPoP proof with access token hash")
        void shouldValidateDPoPProofWithAccessTokenHash() throws Exception {
            String accessToken = "valid_access_token";
            String dPopProofWithAth = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "https://api.example.com/loans", accessToken);

            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

            assertThatCode(() -> dpopProofValidationService.validateDPoPProof(
                    dPopProofWithAth, "GET", "https://api.example.com/loans", accessToken))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should validate DPoP proof with nonce")
        void shouldValidateDPoPProofWithNonce() throws Exception {
            String nonce = "test_nonce_123";
            String dPopProofWithNonce = keyGenerator.createDPoPProofWithNonce(
                    testKeyPair, "POST", "https://auth.example.com/token", nonce);

            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

            assertThatCode(() -> dpopProofValidationService.validateDPoPProofWithNonce(
                    dPopProofWithNonce, "POST", "https://auth.example.com/token", null, nonce))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid DPoP Proof Structure Tests")
    class InvalidDPoPProofStructureTests {

        @Test
        @DisplayName("Should reject DPoP proof with invalid typ header")
        void shouldRejectInvalidTypHeader() throws Exception {
            String invalidProof = keyGenerator.createDPoPProofWithInvalidTyp(testKeyPair, "POST", "https://auth.example.com/token");

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    invalidProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Invalid typ header");
        }

        @Test
        @DisplayName("Should reject DPoP proof with missing JWK")
        void shouldRejectMissingJWK() throws Exception {
            String invalidProof = keyGenerator.createDPoPProofWithoutJWK(testKeyPair, "POST", "https://auth.example.com/token");

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    invalidProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Missing JWK in header");
        }

        @Test
        @DisplayName("Should reject DPoP proof with invalid signature")
        void shouldRejectInvalidSignature() throws Exception {
            String invalidProof = keyGenerator.createDPoPProofWithInvalidSignature(testKeyPair, "POST", "https://auth.example.com/token");

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    invalidProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Invalid signature");
        }

        @Test
        @DisplayName("Should reject DPoP proof with missing required claims")
        void shouldRejectMissingRequiredClaims() throws Exception {
            String invalidProof = keyGenerator.createDPoPProofWithMissingClaims(testKeyPair);

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    invalidProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Missing required claims");
        }
    }

    @Nested
    @DisplayName("HTTP Method and URI Validation Tests")
    class HttpMethodAndUriValidationTests {

        @Test
        @DisplayName("Should reject DPoP proof with mismatched HTTP method")
        void shouldRejectMismatchedHttpMethod() {
            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    validDPoPProof, "GET", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Method mismatch");
        }

        @Test
        @DisplayName("Should reject DPoP proof with mismatched HTTP URI")
        void shouldRejectMismatchedHttpUri() {
            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    validDPoPProof, "POST", "https://different.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("URI mismatch");
        }

        @Test
        @DisplayName("Should validate case-sensitive HTTP method")
        void shouldValidateCaseSensitiveHttpMethod() {
            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    validDPoPProof, "post", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Method mismatch");
        }
    }

    @Nested
    @DisplayName("Timing and Freshness Tests")
    class TimingAndFreshnessTests {

        @Test
        @DisplayName("Should reject DPoP proof that is too old")
        void shouldRejectTooOldProof() throws Exception {
            long oldTimestamp = Instant.now().getEpochSecond() - 120; // 2 minutes ago
            String oldProof = keyGenerator.createDPoPProofWithTimestamp(
                    testKeyPair, "POST", "https://auth.example.com/token", oldTimestamp);

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    oldProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Proof too old");
        }

        @Test
        @DisplayName("Should reject DPoP proof from future")
        void shouldRejectFutureProof() throws Exception {
            long futureTimestamp = Instant.now().getEpochSecond() + 120; // 2 minutes in future
            String futureProof = keyGenerator.createDPoPProofWithTimestamp(
                    testKeyPair, "POST", "https://auth.example.com/token", futureTimestamp);

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    futureProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Proof too far in future");
        }

        @Test
        @DisplayName("Should accept DPoP proof within acceptable clock skew")
        void shouldAcceptProofWithinClockSkew() throws Exception {
            long skewedTimestamp = Instant.now().getEpochSecond() + 25; // 25 seconds in future (within 30s tolerance)
            String skewedProof = keyGenerator.createDPoPProofWithTimestamp(
                    testKeyPair, "POST", "https://auth.example.com/token", skewedTimestamp);

            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

            assertThatCode(() -> dpopProofValidationService.validateDPoPProof(
                    skewedProof, "POST", "https://auth.example.com/token", null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Replay Attack Prevention Tests")
    class ReplayAttackPreventionTests {

        @Test
        @DisplayName("Should reject replayed DPoP proof")
        void shouldRejectReplayedProof() {
            when(redisTemplate.hasKey(anyString())).thenReturn(true);

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    validDPoPProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Proof replay detected");
        }

        @Test
        @DisplayName("Should store JTI for replay prevention")
        void shouldStoreJtiForReplayPrevention() {
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

            dpopProofValidationService.validateDPoPProof(
                    validDPoPProof, "POST", "https://auth.example.com/token", null);

            verify(redisTemplate.opsForValue()).set(anyString(), any(), any());
        }
    }

    @Nested
    @DisplayName("Access Token Hash Tests")
    class AccessTokenHashTests {

        @Test
        @DisplayName("Should reject DPoP proof with incorrect access token hash")
        void shouldRejectIncorrectAccessTokenHash() throws Exception {
            String accessToken = "valid_access_token";
            String wrongAccessToken = "wrong_access_token";
            String dPopProofWithWrongAth = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "https://api.example.com/loans", wrongAccessToken);

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    dPopProofWithWrongAth, "GET", "https://api.example.com/loans", accessToken))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Access token binding mismatch");
        }

        @Test
        @DisplayName("Should require access token hash for resource requests")
        void shouldRequireAccessTokenHashForResourceRequests() throws Exception {
            String accessToken = "valid_access_token";
            String dPopProofWithoutAth = keyGenerator.createValidDPoPProof(
                    testKeyPair, "GET", "https://api.example.com/loans");

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    dPopProofWithoutAth, "GET", "https://api.example.com/loans", accessToken))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Missing access token hash");
        }
    }

    @Nested
    @DisplayName("Key Validation Tests")
    class KeyValidationTests {

        @Test
        @DisplayName("Should reject DPoP proof with weak key")
        void shouldRejectWeakKey() throws Exception {
            ECKey weakKey = keyGenerator.generateWeakECKey();
            String weakProof = keyGenerator.createValidDPoPProof(weakKey, "POST", "https://auth.example.com/token");

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    weakProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Insufficient key strength");
        }

        @Test
        @DisplayName("Should reject DPoP proof with unsupported algorithm")
        void shouldRejectUnsupportedAlgorithm() throws Exception {
            String unsupportedAlgProof = keyGenerator.createDPoPProofWithUnsupportedAlgorithm(
                    testKeyPair, "POST", "https://auth.example.com/token");

            assertThatThrownBy(() -> dpopProofValidationService.validateDPoPProof(
                    unsupportedAlgProof, "POST", "https://auth.example.com/token", null))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Unsupported algorithm");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should validate DPoP proof within acceptable time")
        void shouldValidateWithinAcceptableTime() {
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

            long startTime = System.currentTimeMillis();
            dpopProofValidationService.validateDPoPProof(
                    validDPoPProof, "POST", "https://auth.example.com/token", null);
            long endTime = System.currentTimeMillis();

            assertThat(endTime - startTime).isLessThan(100); // Should complete within 100ms
        }
    }
}