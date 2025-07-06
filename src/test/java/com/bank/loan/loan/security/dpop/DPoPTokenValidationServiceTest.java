package com.bank.loan.loan.security.dpop;

import com.bank.loan.loan.security.dpop.model.DPoPBoundToken;
import com.bank.loan.loan.security.dpop.service.DPoPTokenValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DPoP Token Validation Service Tests")
class DPoPTokenValidationServiceTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private DPoPProofValidationService dpopProofValidationService;

    @InjectMocks
    private DPoPTokenValidationService dpopTokenValidationService;

    private ECKey testKeyPair;
    private String validAccessToken;
    private String validDPoPProof;
    private DPoPTestKeyGenerator keyGenerator;
    private Jwt mockJwt;

    @BeforeEach
    void setUp() throws Exception {
        keyGenerator = new DPoPTestKeyGenerator();
        testKeyPair = keyGenerator.generateECKey();
        validAccessToken = "valid.access.token";
        validDPoPProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                testKeyPair, "GET", "https://api.example.com/loans", validAccessToken);

        mockJwt = createMockJwt();
        when(jwtDecoder.decode(anyString())).thenReturn(mockJwt);
    }

    private Jwt createMockJwt() {
        String jktThumbprint = keyGenerator.calculateJktThumbprint(testKeyPair);
        return Jwt.withTokenValue(validAccessToken)
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
    }

    @Nested
    @DisplayName("Valid DPoP Token Validation Tests")
    class ValidDPoPTokenValidationTests {

        @Test
        @DisplayName("Should validate DPoP-bound token successfully")
        void shouldValidateDPoPBoundToken() {
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    anyString(), anyString(), anyString(), anyString());

            assertThatCode(() -> dpopTokenValidationService.validateDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should extract DPoP bound token information")
        void shouldExtractDPoPBoundTokenInfo() {
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    anyString(), anyString(), anyString(), anyString());

            DPoPBoundToken result = dpopTokenValidationService.validateAndExtractDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans");

            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(validAccessToken);
            assertThat(result.getJwtClaimsSet().getSubject()).isEqualTo("user123");
            assertThat(result.getJktThumbprint()).isEqualTo(keyGenerator.calculateJktThumbprint(testKeyPair));
            assertThat(result.getScopes()).containsExactly("loans", "payments");
        }

        @Test
        @DisplayName("Should validate token binding with matching JKT")
        void shouldValidateTokenBindingWithMatchingJkt() {
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    anyString(), anyString(), anyString(), anyString());

            assertThatCode(() -> dpopTokenValidationService.validateTokenBinding(
                    mockJwt, testKeyPair))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Invalid Token Structure Tests")
    class InvalidTokenStructureTests {

        @Test
        @DisplayName("Should reject token without cnf claim")
        void shouldRejectTokenWithoutCnfClaim() {
            Jwt jwtWithoutCnf = Jwt.withTokenValue(validAccessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("exp", Instant.now().plusSeconds(300))
                    .claim("iat", Instant.now())
                    .claim("jti", "token123")
                    .claim("scope", "loans payments")
                    .build();

            when(jwtDecoder.decode(anyString())).thenReturn(jwtWithoutCnf);

            assertThatThrownBy(() -> dpopTokenValidationService.validateDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans"))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("Token is not DPoP-bound");
        }

        @Test
        @DisplayName("Should reject token with invalid cnf claim structure")
        void shouldRejectTokenWithInvalidCnfClaim() {
            Jwt jwtWithInvalidCnf = Jwt.withTokenValue(validAccessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("exp", Instant.now().plusSeconds(300))
                    .claim("iat", Instant.now())
                    .claim("jti", "token123")
                    .claim("cnf", Map.of("invalid", "claim"))
                    .claim("scope", "loans payments")
                    .build();

            when(jwtDecoder.decode(anyString())).thenReturn(jwtWithInvalidCnf);

            assertThatThrownBy(() -> dpopTokenValidationService.validateDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans"))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("Missing jkt claim");
        }

        @Test
        @DisplayName("Should reject expired token")
        void shouldRejectExpiredToken() {
            String jktThumbprint = keyGenerator.calculateJktThumbprint(testKeyPair);
            Jwt expiredJwt = Jwt.withTokenValue(validAccessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("exp", Instant.now().minusSeconds(300)) // Expired 5 minutes ago
                    .claim("iat", Instant.now().minusSeconds(600))
                    .claim("jti", "token123")
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .claim("scope", "loans payments")
                    .build();

            when(jwtDecoder.decode(anyString())).thenReturn(expiredJwt);

            assertThatThrownBy(() -> dpopTokenValidationService.validateDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans"))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("Token expired");
        }
    }

    @Nested
    @DisplayName("Token Binding Validation Tests")
    class TokenBindingValidationTests {

        @Test
        @DisplayName("Should reject token with mismatched JKT thumbprint")
        void shouldRejectTokenWithMismatchedJkt() throws Exception {
            ECKey differentKeyPair = keyGenerator.generateECKey();
            String differentJktThumbprint = keyGenerator.calculateJktThumbprint(differentKeyPair);
            
            Jwt jwtWithDifferentJkt = Jwt.withTokenValue(validAccessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("exp", Instant.now().plusSeconds(300))
                    .claim("iat", Instant.now())
                    .claim("jti", "token123")
                    .claim("cnf", Map.of("jkt", differentJktThumbprint))
                    .claim("scope", "loans payments")
                    .build();

            when(jwtDecoder.decode(anyString())).thenReturn(jwtWithDifferentJkt);

            assertThatThrownBy(() -> dpopTokenValidationService.validateTokenBinding(
                    jwtWithDifferentJkt, testKeyPair))
                    .isInstanceOf(TokenBindingMismatchException.class)
                    .hasMessageContaining("JKT thumbprint mismatch");
        }

        @Test
        @DisplayName("Should validate token binding with correct JKT")
        void shouldValidateTokenBindingWithCorrectJkt() {
            assertThatCode(() -> dpopTokenValidationService.validateTokenBinding(
                    mockJwt, testKeyPair))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("DPoP Proof Validation Integration Tests")
    class DPoPProofValidationIntegrationTests {

        @Test
        @DisplayName("Should validate DPoP proof with access token")
        void shouldValidateDPoPProofWithAccessToken() {
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    validDPoPProof, "GET", "https://api.example.com/loans", validAccessToken);

            assertThatCode(() -> dpopTokenValidationService.validateDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans"))
                    .doesNotThrowAnyException();

            verify(dpopProofValidationService).validateDPoPProof(
                    validDPoPProof, "GET", "https://api.example.com/loans", validAccessToken);
        }

        @Test
        @DisplayName("Should propagate DPoP proof validation errors")
        void shouldPropagateDPoPProofValidationErrors() {
            doThrow(new InvalidDPoPProofException("Invalid proof"))
                    .when(dpopProofValidationService).validateDPoPProof(
                            anyString(), anyString(), anyString(), anyString());

            assertThatThrownBy(() -> dpopTokenValidationService.validateDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans"))
                    .isInstanceOf(InvalidDPoPProofException.class)
                    .hasMessageContaining("Invalid proof");
        }
    }

    @Nested
    @DisplayName("Token Introspection Tests")
    class TokenIntrospectionTests {

        @Test
        @DisplayName("Should extract token claims correctly")
        void shouldExtractTokenClaimsCorrectly() {
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    anyString(), anyString(), anyString(), anyString());

            DPoPBoundToken result = dpopTokenValidationService.validateAndExtractDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans");

            assertThat(result.getIssuer()).isEqualTo("https://auth.example.com");
            assertThat(result.getSubject()).isEqualTo("user123");
            assertThat(result.getAudience()).containsExactly("https://api.example.com");
            assertThat(result.getTokenId()).isEqualTo("token123");
            assertThat(result.isActive()).isTrue();
            assertThat(result.getExpiresAt()).isAfter(Instant.now());
            assertThat(result.getIssuedAt()).isBefore(Instant.now());
        }

        @Test
        @DisplayName("Should handle token without optional claims")
        void shouldHandleTokenWithoutOptionalClaims() {
            String jktThumbprint = keyGenerator.calculateJktThumbprint(testKeyPair);
            Jwt minimalJwt = Jwt.withTokenValue(validAccessToken)
                    .header("alg", "PS256")
                    .header("typ", "JWT")
                    .claim("iss", "https://auth.example.com")
                    .claim("sub", "user123")
                    .claim("aud", "https://api.example.com")
                    .claim("exp", Instant.now().plusSeconds(300))
                    .claim("iat", Instant.now())
                    .claim("cnf", Map.of("jkt", jktThumbprint))
                    .build();

            when(jwtDecoder.decode(anyString())).thenReturn(minimalJwt);
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    anyString(), anyString(), anyString(), anyString());

            DPoPBoundToken result = dpopTokenValidationService.validateAndExtractDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans");

            assertThat(result.getTokenId()).isNull();
            assertThat(result.getScopes()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should validate DPoP token within acceptable time")
        void shouldValidateWithinAcceptableTime() {
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    anyString(), anyString(), anyString(), anyString());

            long startTime = System.currentTimeMillis();
            dpopTokenValidationService.validateDPoPBoundToken(
                    validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans");
            long endTime = System.currentTimeMillis();

            assertThat(endTime - startTime).isLessThan(50); // Should complete within 50ms
        }

        @Test
        @DisplayName("Should handle concurrent validation requests")
        void shouldHandleConcurrentValidationRequests() {
            doNothing().when(dpopProofValidationService).validateDPoPProof(
                    anyString(), anyString(), anyString(), anyString());

            assertThatCode(() -> {
                for (int i = 0; i < 100; i++) {
                    dpopTokenValidationService.validateDPoPBoundToken(
                            validAccessToken, validDPoPProof, "GET", "https://api.example.com/loans");
                }
            }).doesNotThrowAnyException();
        }
    }
}