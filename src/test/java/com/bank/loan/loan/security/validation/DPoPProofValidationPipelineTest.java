package com.bank.loan.loan.security.validation;

import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Comprehensive DPoP Proof Generation and Validation Pipeline Test
 * Tests the complete DPoP proof lifecycle from generation to validation
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DPoP Proof Generation and Validation Pipeline")
class DPoPProofValidationPipelineTest {

    private DPoPProofValidationService dpopProofValidationService;
    private DPoPClientLibrary.DPoPHttpClient dpopClient;
    
    @MockBean
    private RedisTemplate<String, Object> redisTemplate;
    
    @MockBean
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // No replay by default
        
        this.dpopProofValidationService = new DPoPProofValidationService(redisTemplate);
    }

    @Test
    @DisplayName("Test 1: EC P-256 DPoP Proof Generation and Validation")
    void testECP256DPoPProofGeneration() throws Exception {
        // Generate EC P-256 key pair
        JWK ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient client = new DPoPClientLibrary.DPoPHttpClient(ecKey);
        
        // Generate DPoP proof for token request
        String httpMethod = "POST";
        String httpUri = "https://api.banking.com/oauth2/token";
        String dpopProof = client.getDPoPHeader(httpMethod, httpUri, null);
        
        assertNotNull(dpopProof);
        assertTrue(dpopProof.startsWith("eyJ")); // JWT format
        
        // Validate the generated proof
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, null);
        });
        
        // Verify proof structure
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);
        
        // Check header
        assertEquals("dpop+jwt", signedJWT.getHeader().getType().toString());
        assertEquals(JWSAlgorithm.ES256, signedJWT.getHeader().getAlgorithm());
        assertNotNull(signedJWT.getHeader().getJWK());
        
        // Check claims
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        assertNotNull(claims.getJWTID()); // jti
        assertEquals(httpMethod, claims.getStringClaim("htm"));
        assertEquals(httpUri, claims.getStringClaim("htu"));
        assertNotNull(claims.getIssueTime()); // iat
    }

    @Test
    @DisplayName("Test 2: RSA 2048 DPoP Proof Generation and Validation")
    void testRSA2048DPoPProofGeneration() throws Exception {
        // Generate RSA 2048 key pair
        JWK rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        DPoPClientLibrary.DPoPHttpClient client = new DPoPClientLibrary.DPoPHttpClient(rsaKey);
        
        // Generate DPoP proof for API request with access token
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9...";
        String dpopProof = client.getDPoPHeader(httpMethod, httpUri, accessToken);
        
        assertNotNull(dpopProof);
        
        // Validate the generated proof
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, accessToken);
        });
        
        // Verify proof includes access token hash
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        assertNotNull(claims.getStringClaim("ath")); // Access token hash
        
        // Verify access token hash calculation
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(accessToken.getBytes());
        String expectedAth = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        assertEquals(expectedAth, claims.getStringClaim("ath"));
    }

    @Test
    @DisplayName("Test 3: DPoP Proof Validation with Different Algorithms")
    void testDPoPProofValidationWithDifferentAlgorithms() throws Exception {
        // Test ES256
        testDPoPProofWithAlgorithm(JWSAlgorithm.ES256, DPoPClientLibrary.DPoPKeyManager.generateECKey());
        
        // Test ES384
        JWK ecP384Key = new ECKey.Builder(com.nimbusds.jose.jwk.Curve.P_384, 
                                        generateECKeyPair(com.nimbusds.jose.jwk.Curve.P_384))
                                        .keyID(UUID.randomUUID().toString())
                                        .build();
        testDPoPProofWithAlgorithm(JWSAlgorithm.ES384, ecP384Key);
        
        // Test RS256
        testDPoPProofWithAlgorithm(JWSAlgorithm.RS256, DPoPClientLibrary.DPoPKeyManager.generateRSAKey());
        
        // Test PS256
        testDPoPProofWithAlgorithm(JWSAlgorithm.PS256, DPoPClientLibrary.DPoPKeyManager.generateRSAKey());
    }

    @Test
    @DisplayName("Test 4: JTI Replay Prevention Validation")
    void testJTIReplayPrevention() throws Exception {
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "test_access_token";
        
        // Create first DPoP proof
        String jti1 = UUID.randomUUID().toString();
        String dpopProof1 = createDPoPProof(dpopKey, httpMethod, httpUri, accessToken, jti1);
        
        // First validation should succeed
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof1, httpMethod, httpUri, accessToken);
        });
        
        // Mock JTI as already used
        when(redisTemplate.hasKey("dpop:jti:" + jti1)).thenReturn(true);
        
        // Second validation with same JTI should fail (replay attack)
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(dpopProof1, httpMethod, httpUri, accessToken);
        });
        
        // Different JTI should work
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String jti2 = UUID.randomUUID().toString();
        String dpopProof2 = createDPoPProof(dpopKey, httpMethod, httpUri, accessToken, jti2);
        
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof2, httpMethod, httpUri, accessToken);
        });
    }

    @Test
    @DisplayName("Test 5: DPoP Proof Timestamp Validation")
    void testDPoPProofTimestampValidation() throws Exception {
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "test_access_token";
        
        // Test future timestamp (should fail)
        Instant futureTime = Instant.now().plusSeconds(3600); // 1 hour in future
        String futureDPoPProof = createDPoPProofWithTimestamp(dpopKey, httpMethod, httpUri, accessToken, futureTime);
        
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(futureDPoPProof, httpMethod, httpUri, accessToken);
        });
        
        // Test expired timestamp (should fail)
        Instant pastTime = Instant.now().minusSeconds(3600); // 1 hour in past
        String expiredDPoPProof = createDPoPProofWithTimestamp(dpopKey, httpMethod, httpUri, accessToken, pastTime);
        
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(expiredDPoPProof, httpMethod, httpUri, accessToken);
        });
        
        // Test current timestamp (should succeed)
        Instant currentTime = Instant.now();
        String validDPoPProof = createDPoPProofWithTimestamp(dpopKey, httpMethod, httpUri, accessToken, currentTime);
        
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(validDPoPProof, httpMethod, httpUri, accessToken);
        });
    }

    @Test
    @DisplayName("Test 6: HTTP Method and URI Binding Validation")
    void testHTTPMethodAndURIBinding() throws Exception {
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        
        String httpMethod = "POST";
        String httpUri = "https://api.banking.com/api/v1/payments";
        String accessToken = "test_access_token";
        
        // Create DPoP proof for specific method and URI
        String dpopProof = createDPoPProof(dpopKey, httpMethod, httpUri, accessToken, UUID.randomUUID().toString());
        
        // Validation with correct method and URI should succeed
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, accessToken);
        });
        
        // Validation with wrong method should fail
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, "GET", httpUri, accessToken);
        });
        
        // Validation with wrong URI should fail
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, "https://api.banking.com/api/v1/loans", accessToken);
        });
    }

    @Test
    @DisplayName("Test 7: Access Token Hash Validation")
    void testAccessTokenHashValidation() throws Exception {
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "correct_access_token";
        String wrongAccessToken = "wrong_access_token";
        
        // Create DPoP proof with correct access token hash
        String dpopProof = createDPoPProof(dpopKey, httpMethod, httpUri, accessToken, UUID.randomUUID().toString());
        
        // Validation with correct access token should succeed
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, accessToken);
        });
        
        // Validation with wrong access token should fail
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, wrongAccessToken);
        });
    }

    @Test
    @DisplayName("Test 8: DPoP Key Thumbprint Calculation")
    void testDPoPKeyThumbprintCalculation() throws Exception {
        // Test EC key thumbprint
        JWK ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        String ecThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(ecKey);
        
        assertNotNull(ecThumbprint);
        assertEquals(43, ecThumbprint.length()); // Base64url encoded SHA-256 hash length
        
        // Test RSA key thumbprint
        JWK rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        String rsaThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(rsaKey);
        
        assertNotNull(rsaThumbprint);
        assertEquals(43, rsaThumbprint.length());
        
        // Verify thumbprints are different for different keys
        assertNotEquals(ecThumbprint, rsaThumbprint);
        
        // Verify same key produces same thumbprint
        String ecThumbprint2 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(ecKey);
        assertEquals(ecThumbprint, ecThumbprint2);
    }

    @Test
    @DisplayName("Test 9: DPoP Proof Performance Validation")
    void testDPoPProofPerformanceValidation() throws Exception {
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient client = new DPoPClientLibrary.DPoPHttpClient(dpopKey);
        
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "test_access_token";
        
        // Test DPoP proof generation performance
        long generationStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            String dpopProof = client.getDPoPHeader(httpMethod, httpUri, accessToken);
            assertNotNull(dpopProof);
        }
        
        long generationEndTime = System.currentTimeMillis();
        long generationTime = generationEndTime - generationStartTime;
        
        // Should generate 100 proofs in under 5 seconds
        assertTrue(generationTime < 5000, "DPoP proof generation too slow: " + generationTime + "ms");
        
        // Test DPoP proof validation performance
        String dpopProof = client.getDPoPHeader(httpMethod, httpUri, accessToken);
        
        long validationStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < 100; i++) {
            // Create unique DPoP proof to avoid replay detection
            String uniqueProof = createDPoPProof(dpopKey, httpMethod, httpUri, accessToken, UUID.randomUUID().toString());
            
            assertDoesNotThrow(() -> {
                dpopProofValidationService.validateDPoPProof(uniqueProof, httpMethod, httpUri, accessToken);
            });
        }
        
        long validationEndTime = System.currentTimeMillis();
        long validationTime = validationEndTime - validationStartTime;
        
        // Should validate 100 proofs in under 10 seconds
        assertTrue(validationTime < 10000, "DPoP proof validation too slow: " + validationTime + "ms");
    }

    @Test
    @DisplayName("Test 10: DPoP Proof Error Scenarios")
    void testDPoPProofErrorScenarios() throws Exception {
        JWK dpopKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "test_access_token";
        
        // Test invalid JWT format
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof("invalid.jwt.format", httpMethod, httpUri, accessToken);
        });
        
        // Test missing typ header
        String proofWithoutTyp = createInvalidDPoPProof(dpopKey, httpMethod, httpUri, accessToken, "missing_typ");
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(proofWithoutTyp, httpMethod, httpUri, accessToken);
        });
        
        // Test missing jwk header
        String proofWithoutJwk = createInvalidDPoPProof(dpopKey, httpMethod, httpUri, accessToken, "missing_jwk");
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(proofWithoutJwk, httpMethod, httpUri, accessToken);
        });
        
        // Test missing jti claim
        String proofWithoutJti = createInvalidDPoPProof(dpopKey, httpMethod, httpUri, accessToken, "missing_jti");
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(proofWithoutJti, httpMethod, httpUri, accessToken);
        });
        
        // Test invalid signature
        String proofWithInvalidSignature = createInvalidDPoPProof(dpopKey, httpMethod, httpUri, accessToken, "invalid_signature");
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(proofWithInvalidSignature, httpMethod, httpUri, accessToken);
        });
    }

    // Helper methods
    private void testDPoPProofWithAlgorithm(JWSAlgorithm algorithm, JWK key) throws Exception {
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/test";
        String accessToken = "test_token";
        String jti = UUID.randomUUID().toString();
        
        String dpopProof = createDPoPProofWithAlgorithm(key, algorithm, httpMethod, httpUri, accessToken, jti);
        
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, accessToken);
        });
        
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);
        assertEquals(algorithm, signedJWT.getHeader().getAlgorithm());
    }

    private String createDPoPProof(JWK key, String httpMethod, String httpUri, String accessToken, String jti) throws Exception {
        return createDPoPProofWithTimestamp(key, httpMethod, httpUri, accessToken, jti, Instant.now());
    }

    private String createDPoPProofWithTimestamp(JWK key, String httpMethod, String httpUri, String accessToken, Instant timestamp) throws Exception {
        return createDPoPProofWithTimestamp(key, httpMethod, httpUri, accessToken, UUID.randomUUID().toString(), timestamp);
    }

    private String createDPoPProofWithTimestamp(JWK key, String httpMethod, String httpUri, String accessToken, String jti, Instant timestamp) throws Exception {
        JWSAlgorithm algorithm = key instanceof ECKey ? JWSAlgorithm.ES256 : JWSAlgorithm.RS256;
        return createDPoPProofWithAlgorithm(key, algorithm, httpMethod, httpUri, accessToken, jti, timestamp);
    }

    private String createDPoPProofWithAlgorithm(JWK key, JWSAlgorithm algorithm, String httpMethod, String httpUri, String accessToken, String jti) throws Exception {
        return createDPoPProofWithAlgorithm(key, algorithm, httpMethod, httpUri, accessToken, jti, Instant.now());
    }

    private String createDPoPProofWithAlgorithm(JWK key, JWSAlgorithm algorithm, String httpMethod, String httpUri, String accessToken, String jti, Instant timestamp) throws Exception {
        // Create header
        JWSHeader header = new JWSHeader.Builder(algorithm)
                .type(new com.nimbusds.jose.util.JSONObjectUtils().parse("{\"typ\":\"dpop+jwt\"}"))
                .jwk(key.toPublicJWK())
                .build();
        
        // Create claims
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                .jwtID(jti)
                .claim("htm", httpMethod)
                .claim("htu", httpUri)
                .issueTime(Date.from(timestamp));
        
        // Add access token hash if provided
        if (accessToken != null) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes());
            String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            claimsBuilder.claim("ath", ath);
        }
        
        JWTClaimsSet claims = claimsBuilder.build();
        
        // Create and sign JWT
        SignedJWT signedJWT = new SignedJWT(header, claims);
        JWSSigner signer = createSigner(key, algorithm);
        signedJWT.sign(signer);
        
        return signedJWT.serialize();
    }

    private String createInvalidDPoPProof(JWK key, String httpMethod, String httpUri, String accessToken, String errorType) throws Exception {
        JWSAlgorithm algorithm = key instanceof ECKey ? JWSAlgorithm.ES256 : JWSAlgorithm.RS256;
        
        switch (errorType) {
            case "missing_typ":
                JWSHeader headerWithoutTyp = new JWSHeader.Builder(algorithm)
                        .jwk(key.toPublicJWK())
                        .build();
                return createSignedJWT(headerWithoutTyp, createValidClaims(httpMethod, httpUri, accessToken), key).serialize();
                
            case "missing_jwk":
                JWSHeader headerWithoutJwk = new JWSHeader.Builder(algorithm)
                        .type(new com.nimbusds.jose.util.JSONObjectUtils().parse("{\"typ\":\"dpop+jwt\"}"))
                        .build();
                return createSignedJWT(headerWithoutJwk, createValidClaims(httpMethod, httpUri, accessToken), key).serialize();
                
            case "missing_jti":
                JWTClaimsSet claimsWithoutJti = new JWTClaimsSet.Builder()
                        .claim("htm", httpMethod)
                        .claim("htu", httpUri)
                        .issueTime(new Date())
                        .build();
                return createSignedJWT(createValidHeader(key, algorithm), claimsWithoutJti, key).serialize();
                
            case "invalid_signature":
                // Create a valid JWT but tamper with signature
                String validJWT = createDPoPProof(key, httpMethod, httpUri, accessToken, UUID.randomUUID().toString());
                return validJWT.substring(0, validJWT.lastIndexOf('.')) + ".invalid_signature";
                
            default:
                throw new IllegalArgumentException("Unknown error type: " + errorType);
        }
    }

    private JWSHeader createValidHeader(JWK key, JWSAlgorithm algorithm) throws Exception {
        return new JWSHeader.Builder(algorithm)
                .type(new com.nimbusds.jose.util.JSONObjectUtils().parse("{\"typ\":\"dpop+jwt\"}"))
                .jwk(key.toPublicJWK())
                .build();
    }

    private JWTClaimsSet createValidClaims(String httpMethod, String httpUri, String accessToken) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", httpMethod)
                .claim("htu", httpUri)
                .issueTime(new Date());
        
        if (accessToken != null) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes());
            String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            builder.claim("ath", ath);
        }
        
        return builder.build();
    }

    private SignedJWT createSignedJWT(JWSHeader header, JWTClaimsSet claims, JWK key) throws Exception {
        SignedJWT signedJWT = new SignedJWT(header, claims);
        JWSSigner signer = createSigner(key, header.getAlgorithm());
        signedJWT.sign(signer);
        return signedJWT;
    }

    private JWSSigner createSigner(JWK key, JWSAlgorithm algorithm) throws JOSEException {
        if (key instanceof ECKey) {
            return new ECDSASigner((ECKey) key);
        } else if (key instanceof RSAKey) {
            return new RSASSASigner((RSAKey) key);
        } else {
            throw new IllegalArgumentException("Unsupported key type: " + key.getKeyType());
        }
    }

    private java.security.KeyPair generateECKeyPair(com.nimbusds.jose.jwk.Curve curve) throws Exception {
        java.security.KeyPairGenerator keyPairGenerator = java.security.KeyPairGenerator.getInstance("EC");
        java.security.spec.ECGenParameterSpec spec = new java.security.spec.ECGenParameterSpec(curve.getStdName());
        keyPairGenerator.initialize(spec);
        return keyPairGenerator.generateKeyPair();
    }
}