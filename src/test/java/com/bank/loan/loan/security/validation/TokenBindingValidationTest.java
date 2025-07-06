package com.bank.loan.loan.security.validation;

import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.nimbusds.jose.jwk.JWK;
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
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Token Binding and CNF Claim Validation Test
 * Tests DPoP token binding mechanisms and cnf (confirmation) claim functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Token Binding and CNF Claim Validation")
class TokenBindingValidationTest {

    private DPoPClientLibrary.DPoPHttpClient dpopClient;
    private JWK dpopKeyPair;
    private DPoPProofValidationService dpopProofValidationService;
    
    @MockBean
    private RedisTemplate<String, Object> redisTemplate;
    
    @MockBean
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false); // No replay by default
        
        this.dpopKeyPair = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        this.dpopClient = new DPoPClientLibrary.DPoPHttpClient(dpopKeyPair);
        this.dpopProofValidationService = new DPoPProofValidationService(redisTemplate);
    }

    @Test
    @DisplayName("Test 1: DPoP-bound Access Token Creation with CNF Claim")
    void testDPoPBoundTokenCreation() throws Exception {
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        
        // Simulate access token creation with cnf claim
        String mockAccessToken = createMockDPoPBoundToken(jktThumbprint);
        
        // Parse the mock token to verify cnf claim
        SignedJWT accessTokenJWT = SignedJWT.parse(mockAccessToken);
        JWTClaimsSet claims = accessTokenJWT.getJWTClaimsSet();
        
        // Verify cnf claim structure
        assertNotNull(claims.getClaim("cnf"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> cnfClaim = (Map<String, Object>) claims.getClaim("cnf");
        assertEquals(jktThumbprint, cnfClaim.get("jkt"));
        
        System.out.println("✅ DPoP-bound access token with cnf claim created successfully");
        System.out.println("   JKT thumbprint: " + jktThumbprint);
    }

    @Test
    @DisplayName("Test 2: Token Binding Validation - Matching Keys")
    void testTokenBindingValidationMatching() throws Exception {
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        String mockAccessToken = createMockDPoPBoundToken(jktThumbprint);
        
        // Create DPoP proof with the same key
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String dpopProof = dpopClient.getDPoPHeader(httpMethod, httpUri, mockAccessToken);
        
        // Validation should succeed - same key used for token and proof
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, httpMethod, httpUri, mockAccessToken);
        });
        
        // Verify the DPoP proof contains the correct key
        SignedJWT dpopJWT = SignedJWT.parse(dpopProof);
        JWK proofKey = dpopJWT.getHeader().getJWK();
        String proofJktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(proofKey);
        
        assertEquals(jktThumbprint, proofJktThumbprint);
        
        System.out.println("✅ Token binding validation with matching keys successful");
    }

    @Test
    @DisplayName("Test 3: Token Binding Validation - Mismatched Keys")
    void testTokenBindingValidationMismatch() throws Exception {
        // Create token bound to one key
        String jktThumbprint1 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        String mockAccessToken = createMockDPoPBoundToken(jktThumbprint1);
        
        // Create DPoP proof with a different key
        JWK differentKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient differentClient = new DPoPClientLibrary.DPoPHttpClient(differentKey);
        
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String dpopProofWithDifferentKey = differentClient.getDPoPHeader(httpMethod, httpUri, mockAccessToken);
        
        // Validation should fail - different keys
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(dpopProofWithDifferentKey, httpMethod, httpUri, mockAccessToken);
        });
        
        // Verify the keys are indeed different
        String jktThumbprint2 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(differentKey);
        assertNotEquals(jktThumbprint1, jktThumbprint2);
        
        System.out.println("✅ Token binding validation correctly rejects mismatched keys");
    }

    @Test
    @DisplayName("Test 4: CNF Claim Extraction and Validation")
    void testCNFClaimExtraction() throws Exception {
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        String mockAccessToken = createMockDPoPBoundToken(jktThumbprint);
        
        // Extract cnf claim from access token
        SignedJWT accessTokenJWT = SignedJWT.parse(mockAccessToken);
        JWTClaimsSet claims = accessTokenJWT.getJWTClaimsSet();
        
        @SuppressWarnings("unchecked")
        Map<String, Object> cnfClaim = (Map<String, Object>) claims.getClaim("cnf");
        String extractedJkt = (String) cnfClaim.get("jkt");
        
        // Verify extracted JKT matches the original
        assertEquals(jktThumbprint, extractedJkt);
        
        // Verify JKT thumbprint format and length
        assertEquals(43, extractedJkt.length()); // Base64url SHA-256 length
        assertTrue(extractedJkt.matches("^[A-Za-z0-9_-]+$")); // Base64url characters only
        
        System.out.println("✅ CNF claim extraction and validation successful");
    }

    @Test
    @DisplayName("Test 5: Multiple Token Binding Scenarios")
    void testMultipleTokenBindingScenarios() throws Exception {
        // Scenario 1: Same key, different access tokens
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        String accessToken1 = createMockDPoPBoundToken(jktThumbprint, "sub1", "aud1");
        String accessToken2 = createMockDPoPBoundToken(jktThumbprint, "sub2", "aud2");
        
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/test";
        
        // Both should work with the same DPoP client
        String proof1 = dpopClient.getDPoPHeader(httpMethod, httpUri, accessToken1);
        String proof2 = dpopClient.getDPoPHeader(httpMethod, httpUri, accessToken2);
        
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(proof1, httpMethod, httpUri, accessToken1);
        });
        
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(proof2, httpMethod, httpUri, accessToken2);
        });
        
        // Scenario 2: Cross-token validation should fail
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(proof1, httpMethod, httpUri, accessToken2);
        });
        
        System.out.println("✅ Multiple token binding scenarios validated correctly");
    }

    @Test
    @DisplayName("Test 6: Token Binding with Different Algorithms")
    void testTokenBindingWithDifferentAlgorithms() throws Exception {
        // Test with EC key
        JWK ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        String ecJktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(ecKey);
        String ecBoundToken = createMockDPoPBoundToken(ecJktThumbprint);
        DPoPClientLibrary.DPoPHttpClient ecClient = new DPoPClientLibrary.DPoPHttpClient(ecKey);
        
        // Test with RSA key
        JWK rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        String rsaJktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(rsaKey);
        String rsaBoundToken = createMockDPoPBoundToken(rsaJktThumbprint);
        DPoPClientLibrary.DPoPHttpClient rsaClient = new DPoPClientLibrary.DPoPHttpClient(rsaKey);
        
        String httpMethod = "POST";
        String httpUri = "https://api.banking.com/api/v1/payments";
        
        // Both should work with their respective bound tokens
        String ecProof = ecClient.getDPoPHeader(httpMethod, httpUri, ecBoundToken);
        String rsaProof = rsaClient.getDPoPHeader(httpMethod, httpUri, rsaBoundToken);
        
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(ecProof, httpMethod, httpUri, ecBoundToken);
        });
        
        assertDoesNotThrow(() -> {
            dpopProofValidationService.validateDPoPProof(rsaProof, httpMethod, httpUri, rsaBoundToken);
        });
        
        // Cross-algorithm validation should fail
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(ecProof, httpMethod, httpUri, rsaBoundToken);
        });
        
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(rsaProof, httpMethod, httpUri, ecBoundToken);
        });
        
        System.out.println("✅ Token binding with different algorithms validated correctly");
    }

    @Test
    @DisplayName("Test 7: Token Binding Persistence Across Requests")
    void testTokenBindingPersistence() throws Exception {
        String jktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        String mockAccessToken = createMockDPoPBoundToken(jktThumbprint);
        
        // Make multiple API calls with the same token
        String[] endpoints = {
            "https://api.banking.com/api/v1/loans",
            "https://api.banking.com/api/v1/payments", 
            "https://api.banking.com/api/v1/customers",
            "https://api.banking.com/api/v1/accounts"
        };
        
        String[] methods = {"GET", "POST", "PUT", "DELETE"};
        
        for (int i = 0; i < endpoints.length; i++) {
            String method = methods[i];
            String endpoint = endpoints[i];
            
            String dpopProof = dpopClient.getDPoPHeader(method, endpoint, mockAccessToken);
            
            // Each call should succeed with the same bound token
            assertDoesNotThrow(() -> {
                dpopProofValidationService.validateDPoPProof(dpopProof, method, endpoint, mockAccessToken);
            });
            
            // Verify the same key is consistently used
            SignedJWT dpopJWT = SignedJWT.parse(dpopProof);
            JWK proofKey = dpopJWT.getHeader().getJWK();
            String proofJktThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(proofKey);
            assertEquals(jktThumbprint, proofJktThumbprint);
        }
        
        System.out.println("✅ Token binding persistence across requests validated");
    }

    @Test
    @DisplayName("Test 8: Invalid CNF Claim Scenarios")
    void testInvalidCNFClaimScenarios() throws Exception {
        // Test token without cnf claim
        String tokenWithoutCnf = createMockAccessTokenWithoutCnf();
        String dpopProof = dpopClient.getDPoPHeader("GET", "https://api.test.com", tokenWithoutCnf);
        
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(dpopProof, "GET", "https://api.test.com", tokenWithoutCnf);
        });
        
        // Test token with malformed cnf claim
        String tokenWithMalformedCnf = createMockTokenWithMalformedCnf();
        String dpopProof2 = dpopClient.getDPoPHeader("GET", "https://api.test.com", tokenWithMalformedCnf);
        
        assertThrows(RuntimeException.class, () -> {
            dpopProofValidationService.validateDPoPProof(dpopProof2, "GET", "https://api.test.com", tokenWithMalformedCnf);
        });
        
        System.out.println("✅ Invalid CNF claim scenarios handled correctly");
    }

    // Helper methods
    private String createMockDPoPBoundToken(String jktThumbprint) throws Exception {
        return createMockDPoPBoundToken(jktThumbprint, "test-user", "banking-api");
    }

    private String createMockDPoPBoundToken(String jktThumbprint, String subject, String audience) throws Exception {
        long now = System.currentTimeMillis() / 1000;
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("https://auth.banking.com")
                .subject(subject)
                .audience(audience)
                .expirationTime(new Date((now + 3600) * 1000))
                .issueTime(new Date(now * 1000))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", "banking-loans banking-payments")
                .claim("cnf", Map.of("jkt", jktThumbprint))
                .build();
        
        // Create a mock signed JWT (in real implementation would be properly signed)
        return "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9." + 
               Base64.getUrlEncoder().withoutPadding().encodeToString(claimsSet.toString().getBytes()) +
               ".mock_signature_for_testing";
    }

    private String createMockAccessTokenWithoutCnf() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("https://auth.banking.com")
                .subject("test-user")
                .audience("banking-api")
                .expirationTime(new Date((now + 3600) * 1000))
                .issueTime(new Date(now * 1000))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", "banking-loans")
                // No cnf claim
                .build();
        
        return "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9." + 
               Base64.getUrlEncoder().withoutPadding().encodeToString(claimsSet.toString().getBytes()) +
               ".mock_signature_for_testing";
    }

    private String createMockTokenWithMalformedCnf() throws Exception {
        long now = System.currentTimeMillis() / 1000;
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer("https://auth.banking.com")
                .subject("test-user")
                .audience("banking-api")
                .expirationTime(new Date((now + 3600) * 1000))
                .issueTime(new Date(now * 1000))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", "banking-loans")
                .claim("cnf", "invalid_cnf_format") // Should be an object, not string
                .build();
        
        return "eyJhbGciOiJQUzI1NiIsInR5cCI6IkpXVCJ9." + 
               Base64.getUrlEncoder().withoutPadding().encodeToString(claimsSet.toString().getBytes()) +
               ".mock_signature_for_testing";
    }
}