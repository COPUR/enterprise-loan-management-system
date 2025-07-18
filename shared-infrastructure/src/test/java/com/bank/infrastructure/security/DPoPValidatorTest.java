package com.bank.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for DPoP Validation
 * 
 * Following Red-Green-Refactor cycle:
 * 1. RED: Write failing tests first
 * 2. GREEN: Write minimal code to pass
 * 3. REFACTOR: Improve implementation
 * 
 * Security Guardrails:
 * - All edge cases covered
 * - Negative test cases for security vulnerabilities
 * - Comprehensive attack scenario testing
 * - Performance boundary testing
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DPoP Validator - TDD Security Tests")
class DPoPValidatorTest {
    
    @Mock
    private HttpServletRequest request;
    
    private DPoPValidator dpopValidator;
    private RSAKey rsaKey;
    private RSASSASigner signer;
    
    @BeforeEach
    void setUp() throws Exception {
        // This will fail initially - RED phase
        dpopValidator = new DPoPValidator();
        
        // Setup RSA key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        
        rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
            .privateKey((RSAPrivateKey) keyPair.getPrivate())
            .keyID(UUID.randomUUID().toString())
            .build();
        
        signer = new RSASSASigner(rsaKey);
    }
    
    @Nested
    @DisplayName("RED Phase - Failing Tests")
    class RedPhaseTests {
        
        @Test
        @DisplayName("Should reject null DPoP proof")
        void shouldRejectNullDPoPProof() {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, null, "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("DPoP proof is required");
        }
        
        @Test
        @DisplayName("Should reject empty DPoP proof")
        void shouldRejectEmptyDPoPProof() {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, "", "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("DPoP proof is required");
        }
        
        @Test
        @DisplayName("Should reject malformed DPoP JWT")
        void shouldRejectMalformedDPoPJWT() {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, "malformed.jwt.token", "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("Invalid DPoP proof format");
        }
        
        @Test
        @DisplayName("Should reject DPoP with wrong JWT type")
        void shouldRejectWrongJWTType() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("jwt") // Wrong type - should be "dpop+jwt"
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("Invalid DPoP JWT type");
        }
        
        @Test
        @DisplayName("Should reject DPoP with symmetric algorithm")
        void shouldRejectSymmetricAlgorithm() throws Exception {
            // Arrange - This should FAIL initially
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            // Try to create DPoP with HMAC (symmetric) - should be rejected
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256) // Symmetric algorithm
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("Invalid DPoP algorithm: HS256");
        }
        
        @Test
        @DisplayName("Should reject DPoP without JWK in header")
        void shouldRejectMissingJWK() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                // Missing JWK
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("DPoP JWT missing JWK");
        }
        
        @Test
        @DisplayName("Should reject DPoP with missing required claims")
        void shouldRejectMissingRequiredClaims() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            // Missing required claims: jti, htm, htu, iat
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                // Missing jti
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("Missing or empty jti claim");
        }
        
        @Test
        @DisplayName("Should reject DPoP with HTTP method mismatch")
        void shouldRejectHTTPMethodMismatch() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("POST"); // Request method is POST
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET") // DPoP claims GET but request is POST
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("HTTP method mismatch: expected GET, got POST");
        }
        
        @Test
        @DisplayName("Should reject DPoP with URI mismatch")
        void shouldRejectURIMismatch() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/loans") // Wrong URI
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessageContaining("HTTP URI mismatch");
        }
        
        @Test
        @DisplayName("Should reject DPoP with expired timestamp")
        void shouldRejectExpiredTimestamp() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            // Create timestamp 10 minutes ago (expired)
            Instant expired = Instant.now().minusSeconds(600);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(Date.from(expired))
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("DPoP proof too old");
        }
        
        @Test
        @DisplayName("Should reject DPoP with future timestamp")
        void shouldRejectFutureTimestamp() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            // Create timestamp 10 minutes in future
            Instant future = Instant.now().plusSeconds(600);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(Date.from(future))
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("DPoP proof from future");
        }
        
        @Test
        @DisplayName("Should reject DPoP replay attack")
        void shouldRejectReplayAttack() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            String jti = UUID.randomUUID().toString();
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(jti) // Same JTI for replay
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            // First request should succeed, second should fail
            assertThatCode(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .doesNotThrowAnyException();
            
            // Replay attack - should fail
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("DPoP proof replay detected");
        }
        
        @Test
        @DisplayName("Should reject DPoP with invalid signature")
        void shouldRejectInvalidSignature() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            // Sign with wrong key or don't sign at all
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("Invalid DPoP proof signature");
        }
        
        @Test
        @DisplayName("Should reject DPoP with missing access token hash")
        void shouldRejectMissingAccessTokenHash() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                // Missing ath (access token hash) claim
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer access_token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("Missing ath claim");
        }
        
        @Test
        @DisplayName("Should reject DPoP with access token hash mismatch")
        void shouldRejectAccessTokenHashMismatch() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .claim("ath", "wrong_access_token_hash") // Wrong hash
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer access_token"))
                .isInstanceOf(DPoPValidationException.class)
                .hasMessage("Access token hash mismatch");
        }
    }
    
    @Nested
    @DisplayName("Security Boundary Tests")
    class SecurityBoundaryTests {
        
        @Test
        @DisplayName("Should handle extremely long DPoP proof")
        void shouldHandleExtremelyLongDPoPProof() {
            // Arrange
            String longDPoP = "a".repeat(100000); // 100KB
            
            // Act & Assert - Should handle gracefully
            assertThatThrownBy(() -> dpopValidator.validate(request, longDPoP, "Bearer token"))
                .isInstanceOf(DPoPValidationException.class);
        }
        
        @Test
        @DisplayName("Should handle malicious JTI values")
        void shouldHandleMaliciousJTI() throws Exception {
            // Arrange - SQL injection attempt in JTI
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID("'; DROP TABLE users; --") // SQL injection attempt
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - Should handle safely
            assertThatCode(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .doesNotThrowAnyException(); // Should not crash
        }
        
        @Test
        @DisplayName("Should handle concurrent validation requests")
        void shouldHandleConcurrentValidationRequests() {
            // This tests thread safety - implementation should handle concurrent access
            // Will be implemented in GREEN phase
        }
    }
    
    @Nested
    @DisplayName("Performance Boundary Tests")
    class PerformanceBoundaryTests {
        
        @Test
        @DisplayName("Should validate DPoP within time limit")
        void shouldValidateWithinTimeLimit() throws Exception {
            // Arrange
            when(request.getMethod()).thenReturn("GET");
            when(request.getRequestURI()).thenReturn("/api/v1/customers");
            when(request.getScheme()).thenReturn("https");
            when(request.getServerName()).thenReturn("api.banking.example.com");
            when(request.getServerPort()).thenReturn(443);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type("dpop+jwt")
                .jwk(rsaKey.toPublicJWK())
                .build();
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", "GET")
                .claim("htu", "https://api.banking.example.com/api/v1/customers")
                .issueTime(new Date())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(signer);
            
            // Act & Assert - Should complete within 100ms
            long startTime = System.currentTimeMillis();
            
            // This will fail in RED phase
            assertThatCode(() -> dpopValidator.validate(request, jwt.serialize(), "Bearer token"))
                .doesNotThrowAnyException();
            
            long endTime = System.currentTimeMillis();
            assertThat(endTime - startTime).isLessThan(100L);
        }
    }
}

/**
 * DPoP Validation Exception - Will be created in GREEN phase
 */
class DPoPValidationException extends RuntimeException {
    public DPoPValidationException(String message) {
        super(message);
    }
    
    public DPoPValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}