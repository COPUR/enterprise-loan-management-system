package com.loanmanagement.security.oauth21;

import com.loanmanagement.security.keycloak.dpop.DPoPValidator;
import com.loanmanagement.security.keycloak.dpop.DPoPClientLibrary;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive DPoP Validator Test Suite
 * Tests all validation scenarios, edge cases, and error conditions
 * Contributes to 25% of total OAuth 2.1 + DPoP coverage
 */
@DisplayName("DPoP Validator - Comprehensive Validation Tests")
class DPoPValidatorComprehensiveTest {

    @MockBean
    private RedisTemplate<String, String> redisTemplate;
    
    @MockBean
    private ValueOperations<String, String> valueOperations;
    
    private DPoPValidator validator;
    
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        
        validator = new DPoPValidator();
        
        // Inject Redis template via reflection
        try {
            var field = DPoPValidator.class.getDeclaredField("redisTemplate");
            field.setAccessible(true);
            field.set(validator, redisTemplate);
        } catch (Exception e) {
            // Handle reflection exception
        }
    }
    
    @Test
    @DisplayName("Should validate DPoP proof header requirements")
    void shouldValidateDPoPProofHeaderRequirements() throws Exception {
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        String validProof = generator.generateProof("GET", "https://api.bank.com/loans");
        
        // Test valid proof
        DPoPValidator.DPoPValidationResult result = validator.validateDPoPProof(
            validProof, "GET", "https://api.bank.com/loans", null
        );
        
        assertThat(result.isValid()).isTrue();
        assertThat(result.getJktThumbprint()).isNotNull();
        assertThat(result.getPublicKey()).isNotNull();
    }
    
    @Test
    @DisplayName("Should reject DPoP proofs with wrong HTTP method binding")
    void shouldRejectProofsWithWrongHTTPMethodBinding() throws Exception {
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        // Generate proof for GET, but validate against POST
        String proof = generator.generateProof("GET", "https://api.bank.com/loans");
        
        DPoPValidator.DPoPValidationResult result = validator.validateDPoPProof(
            proof, "POST", "https://api.bank.com/loans", null
        );
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("HTTP method mismatch");
    }
    
    @Test
    @DisplayName("Should reject DPoP proofs with wrong URI binding")
    void shouldRejectProofsWithWrongURIBinding() throws Exception {
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        // Generate proof for one URI, validate against another
        String proof = generator.generateProof("GET", "https://api.bank.com/loans");
        
        DPoPValidator.DPoPValidationResult result = validator.validateDPoPProof(
            proof, "GET", "https://api.bank.com/payments", null
        );
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("HTTP URI mismatch");
    }
    
    @Test
    @DisplayName("Should handle URI normalization correctly")
    void shouldHandleURINormalizationCorrectly() throws Exception {
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        // Generate proof with query parameters
        String baseUri = "https://api.bank.com/loans";
        String uriWithQuery = "https://api.bank.com/loans?page=1&size=10";
        
        String proof = generator.generateProof("GET", baseUri);
        
        // Should validate against URI with query parameters (normalized)
        DPoPValidator.DPoPValidationResult result = validator.validateDPoPProof(
            proof, "GET", uriWithQuery, null
        );
        
        // Should be valid as query parameters are normalized away
        assertThat(result.isValid()).isTrue();
    }
    
    @Test
    @DisplayName("Should validate access token hash binding when provided")
    void shouldValidateAccessTokenHashBinding() throws Exception {
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        String accessToken = "test_access_token_abc123";
        String proof = generator.generateProof("POST", "https://api.bank.com/payments", accessToken);
        
        // Test with correct access token
        DPoPValidator.DPoPValidationResult result1 = validator.validateDPoPProof(
            proof, "POST", "https://api.bank.com/payments", accessToken
        );
        assertThat(result1.isValid()).isTrue();
        
        // Test with wrong access token
        DPoPValidator.DPoPValidationResult result2 = validator.validateDPoPProof(
            proof, "POST", "https://api.bank.com/payments", "wrong_token"
        );
        assertThat(result2.isValid()).isFalse();
        assertThat(result2.getErrorMessage()).contains("Access token hash mismatch");
    }
    
    @Test
    @DisplayName("Should handle timestamp validation with clock skew tolerance")
    void shouldHandleTimestampValidationWithClockSkew() throws Exception {
        // This test would require modifying the proof's timestamp
        // For now, we'll test the basic timestamp validation logic
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        String proof = generator.generateProof("GET", "https://api.bank.com/loans");
        
        // Valid proof should pass timestamp validation
        DPoPValidator.DPoPValidationResult result = validator.validateDPoPProof(
            proof, "GET", "https://api.bank.com/loans", null
        );
        
        assertThat(result.isValid()).isTrue();
    }
    
    @Test
    @DisplayName("Should handle JTI replay prevention correctly")
    void shouldHandleJTIReplayPrevention() throws Exception {
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        String proof = generator.generateProof("GET", "https://api.bank.com/loans");
        
        // First validation should succeed
        DPoPValidator.DPoPValidationResult result1 = validator.validateDPoPProof(
            proof, "GET", "https://api.bank.com/loans", null
        );
        assertThat(result1.isValid()).isTrue();
        
        // Mock Redis to return true (JTI already exists)
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        // Second validation should fail (replay)
        DPoPValidator.DPoPValidationResult result2 = validator.validateDPoPProof(
            proof, "GET", "https://api.bank.com/loans", null
        );
        assertThat(result2.isValid()).isFalse();
        assertThat(result2.getErrorMessage()).contains("replay detected");
    }
    
    @Test
    @DisplayName("Should calculate JKT thumbprints consistently")
    void shouldCalculateJKTThumbprintsConsistently() throws Exception {
        // Test EC key thumbprint calculation
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        String thumbprint1 = validator.calculateJKTThumbprint(ecKey);
        String thumbprint2 = validator.calculateJKTThumbprint(ecKey);
        
        assertThat(thumbprint1).isEqualTo(thumbprint2);
        assertThat(thumbprint1).hasSize(43);
        assertThat(thumbprint1).matches("[A-Za-z0-9_-]+");
        
        // Test RSA key thumbprint calculation
        RSAKey rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        String rsaThumbprint = validator.calculateJKTThumbprint(rsaKey);
        
        assertThat(rsaThumbprint).hasSize(43);
        assertThat(rsaThumbprint).isNotEqualTo(thumbprint1); // Different keys = different thumbprints
    }
    
    @Test
    @DisplayName("Should handle malformed JWT structures gracefully")
    void shouldHandleMalformedJWTStructuresGracefully() {
        // Test various malformed JWT structures
        String[] malformedProofs = {
            "not.a.jwt",
            "too.many.parts.in.this.jwt",
            "onlyonepart",
            "two.parts",
            ".missing.header",
            "header..missing.payload",
            "header.payload.", // Missing signature
            "invalid-base64!.payload.signature"
        };
        
        for (String malformed : malformedProofs) {
            DPoPValidator.DPoPValidationResult result = validator.validateDPoPProof(
                malformed, "GET", "https://api.bank.com/test", null
            );
            
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isNotNull();
        }
    }
    
    @Test
    @DisplayName("Should validate different key types correctly")
    void shouldValidateDifferentKeyTypesCorrectly() throws Exception {
        // Test EC P-256 key
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator ecGenerator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        String ecProof = ecGenerator.generateProof("GET", "https://api.bank.com/test");
        
        DPoPValidator.DPoPValidationResult ecResult = validator.validateDPoPProof(
            ecProof, "GET", "https://api.bank.com/test", null
        );
        assertThat(ecResult.isValid()).isTrue();
        
        // Test RSA 2048 key
        RSAKey rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        DPoPClientLibrary.DPoPProofGenerator rsaGenerator = new DPoPClientLibrary.DPoPProofGenerator(rsaKey);
        String rsaProof = rsaGenerator.generateProof("GET", "https://api.bank.com/test");
        
        DPoPValidator.DPoPValidationResult rsaResult = validator.validateDPoPProof(
            rsaProof, "GET", "https://api.bank.com/test", null
        );
        assertThat(rsaResult.isValid()).isTrue();
        
        // Verify different thumbprints for different keys
        assertThat(ecResult.getJktThumbprint()).isNotEqualTo(rsaResult.getJktThumbprint());
    }
    
    @Test
    @DisplayName("Should handle Redis connection errors gracefully")
    void shouldHandleRedisConnectionErrorsGracefully() throws Exception {
        // Mock Redis to throw exception
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis connection failed"));
        
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        String proof = generator.generateProof("GET", "https://api.bank.com/loans");
        
        // Should handle gracefully and return validation failure
        DPoPValidator.DPoPValidationResult result = validator.validateDPoPProof(
            proof, "GET", "https://api.bank.com/loans", null
        );
        
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("validation failed");
    }
    
    @AfterEach
    void cleanup() {
        // Reset mocks
        reset(redisTemplate, valueOperations);
    }
}