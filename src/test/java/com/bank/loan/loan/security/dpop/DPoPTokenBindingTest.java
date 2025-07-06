package com.bank.loan.loan.security.dpop;

import com.bank.loan.loan.security.dpop.model.DPoPBoundToken;
import com.bank.loan.loan.security.dpop.service.DPoPTokenBindingService;
import com.bank.loan.loan.security.dpop.exception.TokenBindingMismatchException;
import com.bank.loan.loan.security.dpop.util.DPoPTestKeyGenerator;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DPoP Token Binding Service Tests")
class DPoPTokenBindingTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private DPoPTokenBindingService dpopTokenBindingService;

    private ECKey testKeyPair;
    private DPoPTestKeyGenerator keyGenerator;
    private String accessToken;
    private String jktThumbprint;

    @BeforeEach
    void setUp() throws Exception {
        keyGenerator = new DPoPTestKeyGenerator();
        testKeyPair = keyGenerator.generateECKey();
        accessToken = "test.access.token";
        jktThumbprint = keyGenerator.calculateJktThumbprint(testKeyPair);
    }

    @Nested
    @DisplayName("Token Binding Creation Tests")
    class TokenBindingCreationTests {

        @Test
        @DisplayName("Should create DPoP-bound access token with cnf claim")
        void shouldCreateDPoPBoundAccessToken() {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://auth.example.com")
                    .subject("user123")
                    .audience("https://api.example.com")
                    .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                    .issueTime(java.util.Date.from(Instant.now()))
                    .jwtID("token123")
                    .claim("scope", "loans payments")
                    .build();

            Jwt mockJwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("exp", Instant.now().plusSeconds(300))
                    .claim("iat", Instant.now())
                    .claim("jti", "token123")
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .claim("scope", "loans payments")
                    .build();

            when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

            String result = dpopTokenBindingService.createDPoPBoundAccessToken(claimsSet, testKeyPair);

            assertThat(result).isEqualTo(accessToken);
            verify(jwtEncoder).encode(any(JwtEncoderParameters.class));
        }

        @Test
        @DisplayName("Should add cnf claim with correct JKT thumbprint")
        void shouldAddCnfClaimWithCorrectJktThumbprint() {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://auth.example.com")
                    .subject("user123")
                    .build();

            JWTClaimsSet boundClaimsSet = dpopTokenBindingService.addDPoPBinding(claimsSet, testKeyPair);

            assertThat(boundClaimsSet.getClaim("cnf")).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> cnfClaim = (Map<String, Object>) boundClaimsSet.getClaim("cnf");
            assertThat(cnfClaim.get("jkt")).isEqualTo(jktThumbprint);
        }

        @Test
        @DisplayName("Should preserve existing claims when adding DPoP binding")
        void shouldPreserveExistingClaimsWhenAddingDPoPBinding() {
            JWTClaimsSet originalClaimsSet = new JWTClaimsSet.Builder()
                    .issuer("https://auth.example.com")
                    .subject("user123")
                    .audience("https://api.example.com")
                    .expirationTime(java.util.Date.from(Instant.now().plusSeconds(300)))
                    .issueTime(java.util.Date.from(Instant.now()))
                    .jwtID("token123")
                    .claim("scope", "loans payments")
                    .claim("custom_claim", "custom_value")
                    .build();

            JWTClaimsSet boundClaimsSet = dpopTokenBindingService.addDPoPBinding(originalClaimsSet, testKeyPair);

            assertThat(boundClaimsSet.getIssuer()).isEqualTo("https://auth.example.com");
            assertThat(boundClaimsSet.getSubject()).isEqualTo("user123");
            assertThat(boundClaimsSet.getAudience()).containsExactly("https://api.example.com");
            assertThat(boundClaimsSet.getJWTID()).isEqualTo("token123");
            assertThat(boundClaimsSet.getStringClaim("scope")).isEqualTo("loans payments");
            assertThat(boundClaimsSet.getStringClaim("custom_claim")).isEqualTo("custom_value");
        }
    }

    @Nested
    @DisplayName("Token Binding Validation Tests")
    class TokenBindingValidationTests {

        @Test
        @DisplayName("Should validate token binding with matching JKT")
        void shouldValidateTokenBindingWithMatchingJkt() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            assertThatCode(() -> dpopTokenBindingService.validateTokenBinding(jwt, testKeyPair))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject token binding with mismatched JKT")
        void shouldRejectTokenBindingWithMismatchedJkt() throws Exception {
            ECKey differentKeyPair = keyGenerator.generateECKey();
            String differentJktThumbprint = keyGenerator.calculateJktThumbprint(differentKeyPair);

            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("cnf", Map.of("jkt", differentJktThumbprint))
                    .build();

            assertThatThrownBy(() -> dpopTokenBindingService.validateTokenBinding(jwt, testKeyPair))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("JKT thumbprint mismatch");
        }

        @Test
        @DisplayName("Should reject token without cnf claim")
        void shouldRejectTokenWithoutCnfClaim() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .build();

            assertThatThrownBy(() -> dpopTokenBindingService.validateTokenBinding(jwt, testKeyPair))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("Token is not DPoP-bound");
        }

        @Test
        @DisplayName("Should reject token with invalid cnf claim structure")
        void shouldRejectTokenWithInvalidCnfClaimStructure() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("cnf", Map.of("invalid", "claim"))
                    .build();

            assertThatThrownBy(() -> dpopTokenBindingService.validateTokenBinding(jwt, testKeyPair))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("Missing jkt claim");
        }

        @Test
        @DisplayName("Should reject token with empty JKT thumbprint")
        void shouldRejectTokenWithEmptyJktThumbprint() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("cnf", Map.of("jkt", ""))
                    .build();

            assertThatThrownBy(() -> dpopTokenBindingService.validateTokenBinding(jwt, testKeyPair))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("Empty jkt claim");
        }
    }

    @Nested
    @DisplayName("JKT Thumbprint Calculation Tests")
    class JktThumbprintCalculationTests {

        @Test
        @DisplayName("Should calculate consistent JKT thumbprint")
        void shouldCalculateConsistentJktThumbprint() {
            String thumbprint1 = dpopTokenBindingService.calculateJktThumbprint(testKeyPair);
            String thumbprint2 = dpopTokenBindingService.calculateJktThumbprint(testKeyPair);

            assertThat(thumbprint1).isEqualTo(thumbprint2);
            assertThat(thumbprint1).isNotEmpty();
            assertThat(thumbprint1).hasSize(43); // Base64URL encoded SHA-256 hash
        }

        @Test
        @DisplayName("Should calculate different JKT thumbprints for different keys")
        void shouldCalculateDifferentJktThumbprintsForDifferentKeys() throws Exception {
            ECKey differentKeyPair = keyGenerator.generateECKey();

            String thumbprint1 = dpopTokenBindingService.calculateJktThumbprint(testKeyPair);
            String thumbprint2 = dpopTokenBindingService.calculateJktThumbprint(differentKeyPair);

            assertThat(thumbprint1).isNotEqualTo(thumbprint2);
        }

        @Test
        @DisplayName("Should handle RSA keys for JKT thumbprint calculation")
        void shouldHandleRsaKeysForJktThumbprintCalculation() throws Exception {
            var rsaKeyPair = keyGenerator.generateRSAKey();

            String thumbprint = dpopTokenBindingService.calculateJktThumbprint(rsaKeyPair);

            assertThat(thumbprint).isNotEmpty();
            assertThat(thumbprint).hasSize(43);
        }
    }

    @Nested
    @DisplayName("Token Binding Extraction Tests")
    class TokenBindingExtractionTests {

        @Test
        @DisplayName("Should extract DPoP bound token information")
        void shouldExtractDPoPBoundTokenInfo() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("exp", Instant.now().plusSeconds(300))
                    .claim("iat", Instant.now())
                    .claim("jti", "token123")
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .claim("scope", "loans payments")
                    .build();

            DPoPBoundToken boundToken = dpopTokenBindingService.extractDPoPBoundToken(jwt);

            assertThat(boundToken.getAccessToken()).isEqualTo(accessToken);
            assertThat(boundToken.getJktThumbprint()).isEqualTo(jktThumbprint);
            assertThat(boundToken.getIssuer()).isEqualTo("https://auth.example.com");
            assertThat(boundToken.getSubject()).isEqualTo("user123");
            assertThat(boundToken.getAudience()).containsExactly("https://api.example.com");
            assertThat(boundToken.getTokenId()).isEqualTo("token123");
            assertThat(boundToken.getScopes()).containsExactly("loans", "payments");
        }

        @Test
        @DisplayName("Should extract minimal DPoP bound token information")
        void shouldExtractMinimalDPoPBoundTokenInfo() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            DPoPBoundToken boundToken = dpopTokenBindingService.extractDPoPBoundToken(jwt);

            assertThat(boundToken.getAccessToken()).isEqualTo(accessToken);
            assertThat(boundToken.getJktThumbprint()).isEqualTo(jktThumbprint);
            assertThat(boundToken.getIssuer()).isEqualTo("https://auth.example.com");
            assertThat(boundToken.getSubject()).isEqualTo("user123");
            assertThat(boundToken.getAudience()).isEmpty();
            assertThat(boundToken.getTokenId()).isNull();
            assertThat(boundToken.getScopes()).isEmpty();
        }

        @Test
        @DisplayName("Should handle single audience claim")
        void shouldHandleSingleAudienceClaim() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            DPoPBoundToken boundToken = dpopTokenBindingService.extractDPoPBoundToken(jwt);

            assertThat(boundToken.getAudience()).containsExactly("https://api.example.com");
        }
    }

    @Nested
    @DisplayName("Token Binding Lifecycle Tests")
    class TokenBindingLifecycleTests {

        @Test
        @DisplayName("Should validate token binding with active token")
        void shouldValidateTokenBindingWithActiveToken() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("exp", Instant.now().plusSeconds(300))
                    .claim("iat", Instant.now())
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            DPoPBoundToken boundToken = dpopTokenBindingService.extractDPoPBoundToken(jwt);

            assertThat(boundToken.isActive()).isTrue();
        }

        @Test
        @DisplayName("Should detect expired token")
        void shouldDetectExpiredToken() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("exp", Instant.now().minusSeconds(300))
                    .claim("iat", Instant.now().minusSeconds(600))
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            DPoPBoundToken boundToken = dpopTokenBindingService.extractDPoPBoundToken(jwt);

            assertThat(boundToken.isActive()).isFalse();
        }

        @Test
        @DisplayName("Should handle token without expiration")
        void shouldHandleTokenWithoutExpiration() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("iat", Instant.now())
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            DPoPBoundToken boundToken = dpopTokenBindingService.extractDPoPBoundToken(jwt);

            assertThat(boundToken.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should calculate JKT thumbprint efficiently")
        void shouldCalculateJktThumbprintEfficiently() {
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 1000; i++) {
                dpopTokenBindingService.calculateJktThumbprint(testKeyPair);
            }
            
            long endTime = System.currentTimeMillis();
            long averageTime = (endTime - startTime) / 1000;

            assertThat(averageTime).isLessThan(1); // Should average less than 1ms per calculation
        }

        @Test
        @DisplayName("Should validate token binding efficiently")
        void shouldValidateTokenBindingEfficiently() {
            Jwt jwt = Jwt.withTokenValue(accessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 1000; i++) {
                dpopTokenBindingService.validateTokenBinding(jwt, testKeyPair);
            }
            
            long endTime = System.currentTimeMillis();
            long averageTime = (endTime - startTime) / 1000;

            assertThat(averageTime).isLessThan(1); // Should average less than 1ms per validation
        }
    }
}