package com.loanmanagement.shared.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for Authentication Enhancements
 * Tests for OAuth2AuthenticationService and FAPI security components
 */
@DisplayName("Authentication Enhancements - TDD Tests")
class AuthenticationTest {

    private FAPIJwtTokenProvider jwtTokenProvider;
    private FAPISecurityValidator securityValidator;
    private OAuth2AuthenticationService oauth2AuthService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new FAPIJwtTokenProvider();
        securityValidator = new FAPISecurityValidator();
        oauth2AuthService = new OAuth2AuthenticationService(jwtTokenProvider);
    }

    @Test
    @DisplayName("Should authenticate with OAuth2 and return FAPI-compliant JWT")
    void shouldAuthenticateWithOAuth2() {
        // Given: Valid client credentials
        String clientId = "test-client-id";
        String clientSecret = "test-client-secret";
        
        // When: Authenticate client
        OAuth2AuthenticationService.ClientAuthenticationResult authResult = 
            oauth2AuthService.authenticateClient(clientId, clientSecret);
        
        assertThat(authResult.isSuccess()).isTrue();
        
        // When: Issue access token
        OAuth2AuthenticationService.OAuth2TokenResponse tokenResponse = 
            oauth2AuthService.issueAccessToken(clientId, "read write", "test-dpop-thumbprint");
        
        // Then: Return valid JWT token with proper FAPI claims
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.getAccessToken()).isNotNull();
        assertThat(tokenResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(tokenResponse.getExpiresIn()).isGreaterThan(0);
        
        // Validate JWT structure and claims
        JwtTokenClaims claims = jwtTokenProvider.validateToken(tokenResponse.getAccessToken());
        assertThat(claims.getIssuer()).isEqualTo("https://auth-server.example.com");
        assertThat(claims.getAudience()).contains("loan-management-api");
        assertThat(claims.getSubject()).isNotNull();
        assertThat(claims.getIssuedAt()).isBefore(Instant.now());
        assertThat(claims.getExpiresAt()).isAfter(Instant.now());
        
        // FAPI-specific claims should be present
        assertThat(claims.getCustomClaim("auth_time")).isNotNull();
        assertThat(claims.getCustomClaim("acr")).isEqualTo("urn:mace:incommon:iap:silver");
    }

    @Test
    @DisplayName("Should validate access tokens")
    void shouldValidateAccessTokens() {
        // Given: Valid access token
        String accessToken = jwtTokenProvider.generateToken("test-subject", Map.of(
            "client_id", "test-client-id",
            "scope", "read write"
        ));
        
        // When: Validate access token
        OAuth2AuthenticationService.OAuth2TokenValidationResult result = 
            oauth2AuthService.validateAccessToken(accessToken);
        
        // Then: Token should be valid
        assertThat(result.isValid()).isTrue();
        assertThat(result.getClaims()).isNotNull();
        assertThat(result.getClaims().getSubject()).isEqualTo("test-subject");
    }

    @Test
    @DisplayName("Should validate DPoP proofs")
    void shouldValidateDPoPProofs() {
        // Given: DPoP proof JWT
        FAPIJwtTokenProvider.DPoPProof dpopProof = new FAPIJwtTokenProvider.DPoPProof(
            "POST",
            "https://api.example.com/loans",
            Instant.now(),
            "unique-jti-123",
            null // Simplified for test
        );
        String dpopProofJwt = jwtTokenProvider.generateDPoPProof(dpopProof);
        
        // When: Validate DPoP proof
        OAuth2AuthenticationService.DPoPValidationResult result = 
            oauth2AuthService.validateDPoPProof(dpopProofJwt, "POST", "https://api.example.com/loans");
        
        // Then: DPoP proof should be valid
        assertThat(result.isValid()).isTrue();
        assertThat(result.getPublicKeyThumbprint()).isNotNull();
    }

    @Test
    @DisplayName("Should validate FAPI request headers")
    void shouldValidateFAPIRequestHeaders() {
        // Given: FAPI request with required headers
        Map<String, String> headers = Map.of(
            "X-FAPI-Interaction-ID", "550e8400-e29b-41d4-a716-446655440000",
            "Authorization", "Bearer test-token",
            "Content-Type", "application/json"
        );
        FAPISecurityValidator.FAPIRequest request = new FAPISecurityValidator.FAPIRequest(headers, true);
        
        // When: Validate FAPI request
        FAPISecurityValidator.FAPIValidationResult result = securityValidator.validateRequest(request);
        
        // Then: Request should be valid
        assertThat(result.isValid()).isTrue();
        assertThat(result.getViolations()).isEmpty();
    }

    @Test
    @DisplayName("Should reject invalid client credentials")
    void shouldRejectInvalidClientCredentials() {
        // Given: Invalid client credentials
        String clientId = "invalid-client";
        String clientSecret = "wrong-secret";
        
        // When: Authenticate client
        OAuth2AuthenticationService.ClientAuthenticationResult result = 
            oauth2AuthService.authenticateClient(clientId, clientSecret);
        
        // Then: Authentication should fail
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotNull();
    }

    @Test
    @DisplayName("Should reject expired access tokens")
    void shouldRejectExpiredAccessTokens() {
        // Given: Expired access token (simulate by using invalid token)
        String expiredToken = "expired.token.signature";
        
        // When: Validate expired token
        OAuth2AuthenticationService.OAuth2TokenValidationResult result = 
            oauth2AuthService.validateAccessToken(expiredToken);
        
        // Then: Token should be invalid
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("validation failed");
    }
}