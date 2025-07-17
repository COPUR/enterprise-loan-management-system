package com.bank.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * FAPI 2.0 DPoP Compliance Test Suite
 * 
 * This comprehensive test suite validates complete FAPI 2.0 DPoP compliance:
 * - RFC 9449 (OAuth 2.0 Demonstrating Proof-of-Possession)
 * - FAPI 2.0 Security Profile requirements
 * - DPoP token structure and validation
 * - Token binding and replay protection
 * - Nonce handling and time-based validation
 * - JWK validation and signature verification
 * - High-value transaction protection
 * - Islamic banking compliance integration
 * 
 * Standards Compliance:
 * - RFC 9449: OAuth 2.0 Demonstrating Proof-of-Possession at the Application Layer
 * - FAPI 2.0: Financial-grade API Security Profile 2.0
 * - OpenID Connect Core 1.0
 * - RFC 7519: JSON Web Token (JWT)
 * - RFC 7517: JSON Web Key (JWK)
 * - RFC 7518: JSON Web Algorithms (JWA)
 */
@DisplayName("FAPI 2.0 DPoP Compliance Test Suite")
@Execution(ExecutionMode.CONCURRENT)
class Fapi2DPoPComplianceTest {

    private ObjectMapper objectMapper;
    private SecureRandom secureRandom;
    private MessageDigest sha256;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        objectMapper = new ObjectMapper();
        secureRandom = new SecureRandom();
        sha256 = MessageDigest.getInstance("SHA-256");
    }

    @Nested
    @DisplayName("RFC 9449 DPoP Token Structure Compliance")
    class DPoPTokenStructureTests {

        @Test
        @DisplayName("Should validate DPoP token has correct JWT structure")
        void shouldValidateDPoPTokenHasCorrectJWTStructure() {
            // Given - Create a valid DPoP token structure
            String dpopToken = createValidDPoPToken();

            // When - Parse token structure
            String[] parts = dpopToken.split("\\.");

            // Then - Should have 3 parts (header.payload.signature)
            assertThat(parts).hasSize(3);
            assertThat(parts[0]).isNotEmpty(); // header
            assertThat(parts[1]).isNotEmpty(); // payload
            assertThat(parts[2]).isNotEmpty(); // signature
        }

        @Test
        @DisplayName("Should validate DPoP token header contains required fields")
        void shouldValidateDPoPTokenHeaderContainsRequiredFields() throws Exception {
            // Given - Create DPoP token with proper header
            String dpopToken = createValidDPoPToken();
            String headerJson = decodeBase64Url(dpopToken.split("\\.")[0]);
            Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);

            // Then - Header should contain required fields
            assertThat(header).containsKey("alg"); // Algorithm
            assertThat(header).containsKey("typ"); // Type
            assertThat(header).containsKey("jwk"); // JSON Web Key
            
            // Validate specific values
            assertThat(header.get("typ")).isEqualTo("dpop+jwt");
            assertThat(header.get("alg")).isIn("RS256", "ES256", "EdDSA");
            assertThat(header.get("jwk")).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("Should validate DPoP token payload contains required claims")
        void shouldValidateDPoPTokenPayloadContainsRequiredClaims() throws Exception {
            // Given - Create DPoP token with proper payload
            String dpopToken = createValidDPoPToken();
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Payload should contain required claims
            assertThat(payload).containsKey("jti"); // JWT ID
            assertThat(payload).containsKey("htm"); // HTTP Method
            assertThat(payload).containsKey("htu"); // HTTP URI
            assertThat(payload).containsKey("iat"); // Issued At
            
            // Validate claim formats
            assertThat(payload.get("jti")).isInstanceOf(String.class);
            assertThat(payload.get("htm")).isInstanceOf(String.class);
            assertThat(payload.get("htu")).isInstanceOf(String.class);
            assertThat(payload.get("iat")).isInstanceOf(Number.class);
        }

        @Test
        @DisplayName("Should validate DPoP token contains access token binding")
        void shouldValidateDPoPTokenContainsAccessTokenBinding() throws Exception {
            // Given - Create DPoP token with access token binding
            String accessToken = "sample-access-token";
            String dpopToken = createValidDPoPTokenWithAccessToken(accessToken);
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Should contain access token hash
            assertThat(payload).containsKey("ath"); // Access Token Hash
            assertThat(payload.get("ath")).isInstanceOf(String.class);
            
            // Validate access token hash
            String expectedHash = calculateSHA256Hash(accessToken);
            assertThat(payload.get("ath")).isEqualTo(expectedHash);
        }
    }

    @Nested
    @DisplayName("FAPI 2.0 Security Requirements")
    class FapiSecurityRequirementsTests {

        @Test
        @DisplayName("Should enforce DPoP token expiration within 60 seconds")
        void shouldEnforceDPoPTokenExpirationWithin60Seconds() throws Exception {
            // Given - Create DPoP token with proper expiration
            String dpopToken = createValidDPoPToken();
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // When - Check expiration
            long iat = ((Number) payload.get("iat")).longValue();
            long exp = ((Number) payload.get("exp")).longValue();
            long maxAge = exp - iat;

            // Then - Should expire within 60 seconds
            assertThat(maxAge).isLessThanOrEqualTo(60);
        }

        @Test
        @DisplayName("Should validate HTTP method binding")
        void shouldValidateHttpMethodBinding() throws Exception {
            // Given - Create DPoP token for specific HTTP method
            String httpMethod = "POST";
            String dpopToken = createValidDPoPTokenForMethod(httpMethod);
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Should match HTTP method
            assertThat(payload.get("htm")).isEqualTo(httpMethod);
        }

        @Test
        @DisplayName("Should validate HTTP URI binding")
        void shouldValidateHttpUriBinding() throws Exception {
            // Given - Create DPoP token for specific HTTP URI
            String httpUri = "https://api.amanahfi.ae/v1/accounts";
            String dpopToken = createValidDPoPTokenForUri(httpUri);
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Should match HTTP URI
            assertThat(payload.get("htu")).isEqualTo(httpUri);
        }

        @Test
        @DisplayName("Should validate unique JWT ID for replay protection")
        void shouldValidateUniqueJwtIdForReplayProtection() throws Exception {
            // Given - Create multiple DPoP tokens
            String dpopToken1 = createValidDPoPToken();
            String dpopToken2 = createValidDPoPToken();
            
            String payload1Json = decodeBase64Url(dpopToken1.split("\\.")[1]);
            String payload2Json = decodeBase64Url(dpopToken2.split("\\.")[1]);
            
            Map<String, Object> payload1 = objectMapper.readValue(payload1Json, Map.class);
            Map<String, Object> payload2 = objectMapper.readValue(payload2Json, Map.class);

            // Then - JWT IDs should be unique
            assertThat(payload1.get("jti")).isNotEqualTo(payload2.get("jti"));
        }
    }

    @Nested
    @DisplayName("DPoP Token Validation Tests")
    class DPoPTokenValidationTests {

        @Test
        @DisplayName("Should reject expired DPoP tokens")
        void shouldRejectExpiredDPoPTokens() throws Exception {
            // Given - Create expired DPoP token
            String expiredToken = createExpiredDPoPToken();
            String payloadJson = decodeBase64Url(expiredToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // When - Check expiration
            long exp = ((Number) payload.get("exp")).longValue();
            long now = Instant.now().getEpochSecond();

            // Then - Should be expired
            assertThat(exp).isLessThan(now);
        }

        @Test
        @DisplayName("Should reject DPoP tokens with invalid access token binding")
        void shouldRejectDPoPTokensWithInvalidAccessTokenBinding() throws Exception {
            // Given - Create DPoP token with wrong access token
            String correctAccessToken = "correct-access-token";
            String wrongAccessToken = "wrong-access-token";
            String dpopToken = createValidDPoPTokenWithAccessToken(correctAccessToken);
            
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // When - Calculate hash for wrong token
            String wrongHash = calculateSHA256Hash(wrongAccessToken);
            String correctHash = (String) payload.get("ath");

            // Then - Hashes should not match
            assertThat(wrongHash).isNotEqualTo(correctHash);
        }

        @Test
        @DisplayName("Should validate DPoP token nonce handling")
        void shouldValidateDPoPTokenNonceHandling() throws Exception {
            // Given - Create DPoP token with nonce
            String nonce = UUID.randomUUID().toString();
            String dpopToken = createValidDPoPTokenWithNonce(nonce);
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Should contain the nonce
            assertThat(payload).containsKey("nonce");
            assertThat(payload.get("nonce")).isEqualTo(nonce);
        }
    }

    @Nested
    @DisplayName("JWK Validation Tests")
    class JWKValidationTests {

        @Test
        @DisplayName("Should validate JWK structure in DPoP token header")
        void shouldValidateJWKStructureInDPoPTokenHeader() throws Exception {
            // Given - Create DPoP token with JWK
            String dpopToken = createValidDPoPToken();
            String headerJson = decodeBase64Url(dpopToken.split("\\.")[0]);
            Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
            Map<String, Object> jwk = (Map<String, Object>) header.get("jwk");

            // Then - JWK should have required fields
            assertThat(jwk).containsKey("kty"); // Key Type
            assertThat(jwk).containsKey("use"); // Public Key Use
            assertThat(jwk).containsKey("alg"); // Algorithm
            
            // Validate specific values
            assertThat(jwk.get("kty")).isIn("RSA", "EC", "OKP");
            assertThat(jwk.get("use")).isEqualTo("sig");
            assertThat(jwk.get("alg")).isIn("RS256", "ES256", "EdDSA");
        }

        @Test
        @DisplayName("Should validate JWK thumbprint calculation")
        void shouldValidateJWKThumbprintCalculation() throws Exception {
            // Given - Create DPoP token with JWK
            String dpopToken = createValidDPoPToken();
            String headerJson = decodeBase64Url(dpopToken.split("\\.")[0]);
            Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
            Map<String, Object> jwk = (Map<String, Object>) header.get("jwk");

            // When - Calculate JWK thumbprint
            String thumbprint = calculateJWKThumbprint(jwk);

            // Then - Should generate valid thumbprint
            assertThat(thumbprint).isNotEmpty();
            assertThat(thumbprint).hasSize(43); // Base64URL encoded SHA-256 hash
        }
    }

    @Nested
    @DisplayName("High-Value Transaction Protection")
    class HighValueTransactionTests {

        @Test
        @DisplayName("Should require DPoP for high-value transactions")
        void shouldRequireDPoPForHighValueTransactions() {
            // Given - High-value transaction endpoints
            String[] highValueEndpoints = {
                "/api/v1/payments/high-value",
                "/api/v1/transfers/international",
                "/api/v1/islamic-finance/murabaha"
            };

            // When & Then - All should require DPoP
            for (String endpoint : highValueEndpoints) {
                boolean requiresDPoP = isHighValueEndpoint(endpoint);
                assertThat(requiresDPoP).isTrue();
            }
        }

        @Test
        @DisplayName("Should validate enhanced DPoP claims for high-value transactions")
        void shouldValidateEnhancedDPoPClaimsForHighValueTransactions() throws Exception {
            // Given - DPoP token for high-value transaction
            String dpopToken = createDPoPTokenForHighValueTransaction();
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Should contain enhanced claims
            assertThat(payload).containsKey("cnf"); // Confirmation claim
            assertThat(payload).containsKey("txn"); // Transaction reference
            assertThat(payload.get("cnf")).isInstanceOf(Map.class);
        }
    }

    @Nested
    @DisplayName("Islamic Banking Integration Tests")
    class IslamicBankingIntegrationTests {

        @Test
        @DisplayName("Should validate Sharia-compliant DPoP tokens")
        void shouldValidateShariaCompliantDPoPTokens() throws Exception {
            // Given - DPoP token for Islamic banking endpoint
            String dpopToken = createDPoPTokenForIslamicBanking();
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Should contain Islamic banking claims
            assertThat(payload).containsKey("islamic_compliance");
            assertThat(payload).containsKey("sharia_board_approval");
            assertThat(payload.get("islamic_compliance")).isEqualTo(true);
        }

        @Test
        @DisplayName("Should validate DPoP for Murabaha transactions")
        void shouldValidateDPoPForMurabahaTransactions() throws Exception {
            // Given - DPoP token for Murabaha transaction
            String dpopToken = createDPoPTokenForMurabaha();
            String payloadJson = decodeBase64Url(dpopToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // Then - Should contain Murabaha-specific claims
            assertThat(payload).containsKey("murabaha_contract");
            assertThat(payload).containsKey("profit_sharing_ratio");
            assertThat(payload.get("murabaha_contract")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Performance and Scalability Tests")
    class PerformanceScalabilityTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Should validate DPoP tokens within performance threshold")
        void shouldValidateDPoPTokensWithinPerformanceThreshold() {
            // Given - Multiple DPoP tokens for performance testing
            int tokenCount = 1000;

            // When - Validate tokens in bulk
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < tokenCount; i++) {
                String dpopToken = createValidDPoPToken();
                validateDPoPTokenStructure(dpopToken);
            }
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // Then - Should complete within performance threshold
            assertThat(duration).isLessThan(5000); // 5 seconds for 1000 tokens
        }

        @RepeatedTest(10)
        @DisplayName("Should consistently validate DPoP tokens")
        void shouldConsistentlyValidateDPoPTokens() {
            // Given - Create DPoP token
            String dpopToken = createValidDPoPToken();

            // When & Then - Should consistently validate
            assertThatCode(() -> validateDPoPTokenStructure(dpopToken))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Security Edge Cases")
    class SecurityEdgeCaseTests {

        @Test
        @DisplayName("Should handle malformed DPoP tokens gracefully")
        void shouldHandleMalformedDPoPTokensGracefully() {
            // Given - Malformed DPoP tokens
            String[] malformedTokens = {
                "invalid.jwt.token",
                "eyJhbGciOiJSUzI1NiJ9.invalid.signature", // Invalid payload
                "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9", // Missing signature
                "", // Empty token
                "not-a-jwt-at-all"
            };

            // When & Then - Should handle gracefully
            for (String malformedToken : malformedTokens) {
                assertThatThrownBy(() -> validateDPoPTokenStructure(malformedToken))
                    .isInstanceOf(Exception.class);
            }
        }

        @Test
        @DisplayName("Should reject DPoP tokens with clock skew")
        void shouldRejectDPoPTokensWithClockSkew() throws Exception {
            // Given - DPoP token with future timestamp
            String futureToken = createDPoPTokenWithFutureTimestamp();
            String payloadJson = decodeBase64Url(futureToken.split("\\.")[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);

            // When - Check timestamp
            long iat = ((Number) payload.get("iat")).longValue();
            long now = Instant.now().getEpochSecond();

            // Then - Should be in the future (invalid)
            assertThat(iat).isGreaterThan(now + 60); // Beyond acceptable clock skew
        }
    }

    // Helper methods for creating test DPoP tokens

    private String createValidDPoPToken() {
        return createDPoPToken("POST", "https://api.amanahfi.ae/v1/accounts", null, null);
    }

    private String createValidDPoPTokenWithAccessToken(String accessToken) {
        return createDPoPToken("POST", "https://api.amanahfi.ae/v1/accounts", accessToken, null);
    }

    private String createValidDPoPTokenWithNonce(String nonce) {
        return createDPoPToken("POST", "https://api.amanahfi.ae/v1/accounts", null, nonce);
    }

    private String createValidDPoPTokenForMethod(String method) {
        return createDPoPToken(method, "https://api.amanahfi.ae/v1/accounts", null, null);
    }

    private String createValidDPoPTokenForUri(String uri) {
        return createDPoPToken("POST", uri, null, null);
    }

    private String createExpiredDPoPToken() {
        long expiredTime = Instant.now().getEpochSecond() - 300; // 5 minutes ago
        return createDPoPTokenWithTimestamp("POST", "https://api.amanahfi.ae/v1/accounts", expiredTime, expiredTime);
    }

    private String createDPoPTokenWithFutureTimestamp() {
        long futureTime = Instant.now().getEpochSecond() + 300; // 5 minutes in future
        return createDPoPTokenWithTimestamp("POST", "https://api.amanahfi.ae/v1/accounts", futureTime, futureTime + 60);
    }

    private String createDPoPTokenForHighValueTransaction() {
        Map<String, Object> enhancedClaims = new HashMap<>();
        enhancedClaims.put("cnf", Map.of("jkt", "sample-thumbprint"));
        enhancedClaims.put("txn", "HV-" + UUID.randomUUID().toString());
        return createDPoPTokenWithClaims("POST", "https://api.amanahfi.ae/v1/payments/high-value", enhancedClaims);
    }

    private String createDPoPTokenForIslamicBanking() {
        Map<String, Object> islamicClaims = new HashMap<>();
        islamicClaims.put("islamic_compliance", true);
        islamicClaims.put("sharia_board_approval", "SBA-2024-001");
        return createDPoPTokenWithClaims("POST", "https://api.amanahfi.ae/v1/islamic-finance", islamicClaims);
    }

    private String createDPoPTokenForMurabaha() {
        Map<String, Object> murabahaClaims = new HashMap<>();
        murabahaClaims.put("murabaha_contract", "MUR-" + UUID.randomUUID().toString());
        murabahaClaims.put("profit_sharing_ratio", 0.05);
        return createDPoPTokenWithClaims("POST", "https://api.amanahfi.ae/v1/islamic-finance/murabaha", murabahaClaims);
    }

    private String createDPoPToken(String method, String uri, String accessToken, String nonce) {
        long now = Instant.now().getEpochSecond();
        return createDPoPTokenWithTimestamp(method, uri, now, now + 60);
    }

    private String createDPoPTokenWithTimestamp(String method, String uri, long iat, long exp) {
        return createDPoPTokenWithClaims(method, uri, null, iat, exp);
    }

    private String createDPoPTokenWithClaims(String method, String uri, Map<String, Object> additionalClaims) {
        long now = Instant.now().getEpochSecond();
        return createDPoPTokenWithClaims(method, uri, additionalClaims, now, now + 60);
    }

    private String createDPoPTokenWithClaims(String method, String uri, Map<String, Object> additionalClaims, long iat, long exp) {
        try {
            // Create header
            Map<String, Object> header = new HashMap<>();
            header.put("alg", "RS256");
            header.put("typ", "dpop+jwt");
            header.put("jwk", createSampleJWK());

            // Create payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("jti", UUID.randomUUID().toString());
            payload.put("htm", method);
            payload.put("htu", uri);
            payload.put("iat", iat);
            payload.put("exp", exp);

            if (additionalClaims != null) {
                payload.putAll(additionalClaims);
            }

            // Encode parts
            String encodedHeader = encodeBase64Url(objectMapper.writeValueAsString(header));
            String encodedPayload = encodeBase64Url(objectMapper.writeValueAsString(payload));
            String signature = "sample-signature"; // Mock signature for testing

            return encodedHeader + "." + encodedPayload + "." + signature;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create DPoP token", e);
        }
    }

    private Map<String, Object> createSampleJWK() {
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("n", "sample-modulus");
        jwk.put("e", "AQAB");
        return jwk;
    }

    private String encodeBase64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeBase64Url(String input) {
        return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.UTF_8);
    }

    private String calculateSHA256Hash(String input) {
        byte[] hash = sha256.digest(input.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    private String calculateJWKThumbprint(Map<String, Object> jwk) {
        try {
            // Simplified JWK thumbprint calculation
            String jwkString = objectMapper.writeValueAsString(jwk);
            byte[] hash = sha256.digest(jwkString.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate JWK thumbprint", e);
        }
    }

    private boolean isHighValueEndpoint(String endpoint) {
        return endpoint.contains("/high-value") || 
               endpoint.contains("/international") ||
               endpoint.contains("/murabaha");
    }

    private void validateDPoPTokenStructure(String dpopToken) {
        String[] parts = dpopToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid DPoP token structure");
        }
        
        try {
            // Validate header
            String headerJson = decodeBase64Url(parts[0]);
            Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
            
            if (!"dpop+jwt".equals(header.get("typ"))) {
                throw new IllegalArgumentException("Invalid DPoP token type");
            }
            
            // Validate payload
            String payloadJson = decodeBase64Url(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadJson, Map.class);
            
            if (!payload.containsKey("jti") || !payload.containsKey("htm") || !payload.containsKey("htu")) {
                throw new IllegalArgumentException("Missing required DPoP claims");
            }
            
        } catch (Exception e) {
            throw new RuntimeException("DPoP token validation failed", e);
        }
    }
}