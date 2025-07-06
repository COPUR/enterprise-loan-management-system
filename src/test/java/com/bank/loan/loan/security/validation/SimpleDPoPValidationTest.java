package com.bank.loan.loan.security.validation;

import com.bank.loan.loan.security.dpop.client.DPoPClientLibrary;
import com.bank.loan.loan.security.dpop.service.DPoPProofValidationService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple DPoP Validation Test without Spring Boot dependencies
 * Tests core DPoP functionality to verify implementation correctness
 */
@DisplayName("Simple DPoP Validation Test")
class SimpleDPoPValidationTest {

    private DPoPClientLibrary.DPoPHttpClient dpopClient;
    private JWK dpopKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        // Generate DPoP key pair for testing
        this.dpopKeyPair = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        this.dpopClient = new DPoPClientLibrary.DPoPHttpClient(dpopKeyPair);
    }

    @Test
    @DisplayName("Test DPoP Client Library Key Generation")
    void testDPoPKeyGeneration() throws Exception {
        // Test EC key generation
        JWK ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        assertNotNull(ecKey);
        assertEquals("EC", ecKey.getKeyType().getValue());
        assertNotNull(ecKey.getKeyID());
        
        // Test RSA key generation
        JWK rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        assertNotNull(rsaKey);
        assertEquals("RSA", rsaKey.getKeyType().getValue());
        assertNotNull(rsaKey.getKeyID());
        
        // Verify keys are different
        assertNotEquals(ecKey.getKeyID(), rsaKey.getKeyID());
    }

    @Test
    @DisplayName("Test DPoP JKT Thumbprint Calculation")
    void testJktThumbprintCalculation() throws Exception {
        String thumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        
        assertNotNull(thumbprint);
        assertEquals(43, thumbprint.length()); // Base64url encoded SHA-256 hash length
        
        // Verify same key produces same thumbprint
        String thumbprint2 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(dpopKeyPair);
        assertEquals(thumbprint, thumbprint2);
    }

    @Test
    @DisplayName("Test DPoP Proof Generation - Token Request")
    void testDPoPProofGenerationForToken() throws Exception {
        String httpMethod = "POST";
        String httpUri = "https://api.banking.com/oauth2/token";
        
        String dpopProof = dpopClient.getDPoPHeader(httpMethod, httpUri, null);
        
        assertNotNull(dpopProof);
        assertTrue(dpopProof.startsWith("eyJ")); // JWT format
        
        // Parse and verify structure
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);
        
        // Verify header
        assertEquals("dpop+jwt", signedJWT.getHeader().getType().toString());
        assertEquals(JWSAlgorithm.ES256, signedJWT.getHeader().getAlgorithm());
        assertNotNull(signedJWT.getHeader().getJWK());
        
        // Verify claims
        assertNotNull(signedJWT.getJWTClaimsSet().getJWTID()); // jti
        assertEquals(httpMethod, signedJWT.getJWTClaimsSet().getStringClaim("htm"));
        assertEquals(httpUri, signedJWT.getJWTClaimsSet().getStringClaim("htu"));
        assertNotNull(signedJWT.getJWTClaimsSet().getIssueTime()); // iat
    }

    @Test
    @DisplayName("Test DPoP Proof Generation - API Request with Access Token")
    void testDPoPProofGenerationForAPI() throws Exception {
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "test_access_token_12345";
        
        String dpopProof = dpopClient.getDPoPHeader(httpMethod, httpUri, accessToken);
        
        assertNotNull(dpopProof);
        
        // Parse and verify structure
        SignedJWT signedJWT = SignedJWT.parse(dpopProof);
        
        // Verify access token hash is present
        assertNotNull(signedJWT.getJWTClaimsSet().getStringClaim("ath"));
        
        // Verify other claims
        assertEquals(httpMethod, signedJWT.getJWTClaimsSet().getStringClaim("htm"));
        assertEquals(httpUri, signedJWT.getJWTClaimsSet().getStringClaim("htu"));
    }

    @Test
    @DisplayName("Test DPoP Proof Uniqueness")
    void testDPoPProofUniqueness() throws Exception {
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/loans";
        String accessToken = "test_token";
        
        // Generate multiple proofs
        String proof1 = dpopClient.getDPoPHeader(httpMethod, httpUri, accessToken);
        Thread.sleep(10); // Ensure different timestamps
        String proof2 = dpopClient.getDPoPHeader(httpMethod, httpUri, accessToken);
        
        assertNotNull(proof1);
        assertNotNull(proof2);
        assertNotEquals(proof1, proof2); // Should be different due to jti and iat
        
        // Verify both have different JTIs
        SignedJWT jwt1 = SignedJWT.parse(proof1);
        SignedJWT jwt2 = SignedJWT.parse(proof2);
        
        assertNotEquals(jwt1.getJWTClaimsSet().getJWTID(), jwt2.getJWTClaimsSet().getJWTID());
    }

    @Test
    @DisplayName("Test DPoP Key Binding Consistency")
    void testDPoPKeyBindingConsistency() throws Exception {
        String httpMethod = "POST";
        String httpUri = "https://api.banking.com/api/v1/payments";
        
        // Generate multiple proofs with same key
        String proof1 = dpopClient.getDPoPHeader(httpMethod, httpUri, null);
        String proof2 = dpopClient.getDPoPHeader(httpMethod, httpUri, null);
        
        SignedJWT jwt1 = SignedJWT.parse(proof1);
        SignedJWT jwt2 = SignedJWT.parse(proof2);
        
        // Both should use the same public key
        assertEquals(jwt1.getHeader().getJWK().toJSONString(), 
                    jwt2.getHeader().getJWK().toJSONString());
        
        // JKT thumbprints should be the same
        String jkt1 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(jwt1.getHeader().getJWK());
        String jkt2 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(jwt2.getHeader().getJWK());
        assertEquals(jkt1, jkt2);
    }

    @Test
    @DisplayName("Test Different Keys Produce Different Thumbprints")
    void testDifferentKeysProduceDifferentThumbprints() throws Exception {
        JWK key1 = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        JWK key2 = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        
        String thumbprint1 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(key1);
        String thumbprint2 = DPoPClientLibrary.DPoPKeyManager.calculateJktThumbprint(key2);
        
        assertNotEquals(thumbprint1, thumbprint2);
    }

    @Test
    @DisplayName("Test Multiple Algorithm Support")
    void testMultipleAlgorithmSupport() throws Exception {
        // Test EC key
        JWK ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPHttpClient ecClient = new DPoPClientLibrary.DPoPHttpClient(ecKey);
        
        String ecProof = ecClient.getDPoPHeader("GET", "https://api.test.com", null);
        SignedJWT ecJWT = SignedJWT.parse(ecProof);
        assertEquals(JWSAlgorithm.ES256, ecJWT.getHeader().getAlgorithm());
        
        // Test RSA key
        JWK rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        DPoPClientLibrary.DPoPHttpClient rsaClient = new DPoPClientLibrary.DPoPHttpClient(rsaKey);
        
        String rsaProof = rsaClient.getDPoPHeader("GET", "https://api.test.com", null);
        SignedJWT rsaJWT = SignedJWT.parse(rsaProof);
        assertEquals(JWSAlgorithm.RS256, rsaJWT.getHeader().getAlgorithm());
    }

    @Test
    @DisplayName("Test DPoP Proof Performance")
    void testDPoPProofPerformance() throws Exception {
        String httpMethod = "GET";
        String httpUri = "https://api.banking.com/api/v1/test";
        String accessToken = "test_token";
        
        long startTime = System.currentTimeMillis();
        
        // Generate 50 proofs
        for (int i = 0; i < 50; i++) {
            String proof = dpopClient.getDPoPHeader(httpMethod, httpUri, accessToken);
            assertNotNull(proof);
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Should generate 50 proofs quickly (under 2 seconds)
        assertTrue(totalTime < 2000, "DPoP proof generation too slow: " + totalTime + "ms for 50 proofs");
        
        System.out.println("Generated 50 DPoP proofs in " + totalTime + "ms");
    }
}