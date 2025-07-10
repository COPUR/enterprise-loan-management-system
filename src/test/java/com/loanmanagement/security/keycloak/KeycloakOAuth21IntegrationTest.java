package com.loanmanagement.security.keycloak;

import com.loanmanagement.security.keycloak.dpop.DPoPValidator;
import com.loanmanagement.security.keycloak.fapi.FapiComplianceValidator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Keycloak OAuth 2.1 with FAPI 2.0 and DPoP support
 * Tests the complete authentication and authorization flow
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Keycloak OAuth 2.1 Integration Tests")
class KeycloakOAuth21IntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private DPoPValidator dpopValidator;

    @MockBean
    private FapiComplianceValidator fapiValidator;

    private MockMvc mockMvc;
    private RSAKey rsaKey;
    private JWSSigner signer;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // Generate RSA key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        rsaKey = new RSAKeyGenerator(2048)
            .keyID("test-key-id")
            .keyPair(keyPair)
            .generate();
            
        signer = new RSASSASigner(rsaKey);
    }

    @Test
    @DisplayName("Should allow access to public endpoints without authentication")
    void shouldAllowAccessToPublicEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should require authentication for protected endpoints")
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/loans"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":1000}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should allow authenticated access to protected endpoints")
    void shouldAllowAuthenticatedAccessToProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "Bearer " + createValidAccessToken()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should validate DPoP proof for protected resources")
    void shouldValidateDPoPProofForProtectedResources() throws Exception {
        // Given
        String accessToken = createValidAccessToken();
        String dpopProof = createValidDPoPProof("GET", "https://api.loanmanagement.com/api/v1/loans", accessToken);

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "Bearer " + accessToken)
                .header("DPoP", dpopProof))
            .andExpect(status().isOk())
            .andExpect(request().attribute("dpop.validated", true));
    }

    @Test
    @DisplayName("Should reject invalid DPoP proof")
    void shouldRejectInvalidDPoPProof() throws Exception {
        // Given
        String accessToken = createValidAccessToken();
        String invalidDpopProof = "invalid.dpop.proof";

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "Bearer " + accessToken)
                .header("DPoP", invalidDpopProof))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("dpop_validation_failed"));
    }

    @Test
    @DisplayName("Should validate FAPI compliance headers")
    void shouldValidateFapiComplianceHeaders() throws Exception {
        // Given
        String accessToken = createValidAccessToken();

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "Bearer " + accessToken)
                .secure(true)) // HTTPS required for FAPI
            .andExpect(status().isOk())
            .andExpected(header().string("Cache-Control", "no-store"))
            .andExpected(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("Should handle OAuth2 authorization code flow")
    void shouldHandleOAuth2AuthorizationCodeFlow() throws Exception {
        // Test authorization endpoint with FAPI parameters
        mockMvc.perform(get("/oauth2/authorization/keycloak")
                .param("response_type", "code")
                .param("client_id", "loan-management-system")
                .param("redirect_uri", "https://api.loanmanagement.com/login/oauth2/code/keycloak")
                .param("scope", "openid profile banking")
                .param("state", "secure-random-state-value")
                .param("nonce", "secure-random-nonce-value")
                .param("code_challenge", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
                .param("code_challenge_method", "S256")
                .secure(true))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should handle token exchange with client_credentials grant")
    void shouldHandleTokenExchangeWithClientCredentials() throws Exception {
        // Given
        String clientAssertion = createClientAssertion();

        // When & Then
        mockMvc.perform(post("/oauth2/token")
                .param("grant_type", "client_credentials")
                .param("scope", "banking loans")
                .param("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer")
                .param("client_assertion", clientAssertion)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .secure(true))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").exists())
            .andExpected(jsonPath("$.token_type").value("DPoP"))
            .andExpected(jsonPath("$.expires_in").exists());
    }

    @Test
    @DisplayName("Should validate access token lifetime according to FAPI")
    void shouldValidateAccessTokenLifetimeAccordingToFapi() throws Exception {
        // Given - token with excessive lifetime
        String longLivedToken = createAccessTokenWithLifetime(7200); // 2 hours

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                .header("Authorization", "Bearer " + longLivedToken))
            .andExpect(status().isUnauthorized())
            .andExpected(jsonPath("$.error").value("fapi_compliance_violation"))
            .andExpected(jsonPath("$.error_description").value(containsString("token lifetime")));
    }

    @Test
    @DisplayName("Should handle PKCE validation for authorization code flow")
    void shouldHandlePkceValidationForAuthorizationCodeFlow() throws Exception {
        // Test without PKCE parameters (should fail with FAPI enabled)
        mockMvc.perform(get("/oauth2/authorization/keycloak")
                .param("response_type", "code")
                .param("client_id", "loan-management-system")
                .param("redirect_uri", "https://api.loanmanagement.com/login/oauth2/code/keycloak")
                .param("scope", "openid profile")
                .param("state", "state-value")
                .secure(true))
            .andExpect(status().isBadRequest())
            .andExpected(jsonPath("$.error").value("invalid_request"))
            .andExpected(jsonPath("$.error_description").value(containsString("PKCE")));
    }

    @Test
    @DisplayName("Should support Pushed Authorization Requests (PAR)")
    void shouldSupportPushedAuthorizationRequests() throws Exception {
        // Given
        String requestObject = createSignedRequestObject();

        // When - Push authorization request
        mockMvc.perform(post("/oauth2/par")
                .param("request", requestObject)
                .param("client_id", "loan-management-system")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .secure(true))
            .andExpect(status().isCreated())
            .andExpected(jsonPath("$.request_uri").exists())
            .andExpected(jsonPath("$.expires_in").exists());
    }

    @Test
    @DisplayName("Should handle logout with proper session cleanup")
    void shouldHandleLogoutWithProperSessionCleanup() throws Exception {
        // Given
        String accessToken = createValidAccessToken();

        // When & Then
        mockMvc.perform(post("/oauth2/logout")
                .header("Authorization", "Bearer " + accessToken)
                .param("post_logout_redirect_uri", "https://api.loanmanagement.com/logout/success")
                .secure(true))
            .andExpect(status().is3xxRedirection())
            .andExpected(redirectedUrlPattern("**/logout/success"));
    }

    // Helper methods for creating test tokens and requests

    private String createValidAccessToken() throws JOSEException {
        return createAccessTokenWithLifetime(3600); // 1 hour
    }

    private String createAccessTokenWithLifetime(int lifetimeSeconds) throws JOSEException {
        Instant now = Instant.now();
        
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("test-user")
            .issuer("https://auth.loanmanagement.com/auth/realms/banking")
            .audience("loan-management-system")
            .claim("azp", "loan-management-system")
            .claim("scope", "openid profile banking loans")
            .claim("preferred_username", "testuser")
            .claim("email", "test@example.com")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(lifetimeSeconds)))
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.getKeyID())
            .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        
        return jwt.serialize();
    }

    private String createValidDPoPProof(String method, String uri, String accessToken) throws JOSEException {
        Instant now = Instant.now();
        
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .jwtID(UUID.randomUUID().toString())
            .claim("htm", method)
            .claim("htu", uri)
            .claim("ath", calculateAccessTokenHash(accessToken))
            .issueTime(Date.from(now))
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .type(new com.nimbusds.jose.util.JSONObjectUtils.JSONObjectType("dpop+jwt"))
            .jwk(rsaKey.toPublicJWK())
            .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        
        return jwt.serialize();
    }

    private String createClientAssertion() throws JOSEException {
        Instant now = Instant.now();
        
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer("loan-management-system")
            .subject("loan-management-system")
            .audience("https://auth.loanmanagement.com/auth/realms/banking/protocol/openid-connect/token")
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(300))) // 5 minutes
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.getKeyID())
            .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        
        return jwt.serialize();
    }

    private String createSignedRequestObject() throws JOSEException {
        Instant now = Instant.now();
        
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .claim("client_id", "loan-management-system")
            .claim("response_type", "code")
            .claim("redirect_uri", "https://api.loanmanagement.com/login/oauth2/code/keycloak")
            .claim("scope", "openid profile banking")
            .claim("state", "secure-state-value")
            .claim("nonce", "secure-nonce-value")
            .claim("code_challenge", "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
            .claim("code_challenge_method", "S256")
            .audience("https://auth.loanmanagement.com/auth/realms/banking")
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(300)))
            .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.getKeyID())
            .build();

        SignedJWT jwt = new SignedJWT(header, claims);
        jwt.sign(signer);
        
        return jwt.serialize();
    }

    private String calculateAccessTokenHash(String accessToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes("UTF-8"));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate access token hash", e);
        }
    }
}