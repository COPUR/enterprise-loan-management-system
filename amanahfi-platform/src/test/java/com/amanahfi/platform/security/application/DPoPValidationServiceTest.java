package com.amanahfi.platform.security.application;

import com.amanahfi.platform.security.domain.DPoPToken;
import com.amanahfi.platform.security.port.out.DPoPNonceStore;
import com.amanahfi.platform.security.port.out.JWKValidationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD tests for DPoPValidationService with comprehensive security validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DPoPValidationService Security Validation")
class DPoPValidationServiceTest {
    
    @Mock
    private JWKValidationClient jwkValidationClient;
    
    @Mock
    private DPoPNonceStore nonceStore;
    
    private DPoPValidationService dpopValidationService;
    private DPoPToken validDPoPToken;
    private Map<String, Object> validJWK;
    
    @BeforeEach
    void setUp() {
        dpopValidationService = new DPoPValidationService(jwkValidationClient, nonceStore);
        
        // Setup valid JWK
        validJWK = new HashMap<>();
        validJWK.put("kty", "RSA");
        validJWK.put("use", "sig");
        validJWK.put("alg", "RS256");
        validJWK.put("n", "example-modulus");
        validJWK.put("e", "AQAB");
        
        // Setup valid DPoP token
        Map<String, Object> header = new HashMap<>();
        header.put("typ", "dpop+jwt");
        header.put("alg", "RS256");
        header.put("jwk", validJWK);
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("jti", "test-token-id-123");
        payload.put("htm", "POST");
        payload.put("htu", "https://amanahfi.ae/api/v1/islamic-finance/murabaha");
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plusSeconds(300).getEpochSecond());
        payload.put("ath", "fUHyO2r2Z3DZ53EsNrWBb0xWXoaNy59IiKCAqksmQEo");
        payload.put("nonce", "test-nonce-456");
        
        validDPoPToken = DPoPToken.builder()
            .header(header)
            .payload(payload)
            .signature("example-signature")
            .httpMethod("POST")
            .httpUri("https://amanahfi.ae/api/v1/islamic-finance/murabaha")
            .jwkThumbprint("example-thumbprint")
            .rawToken("header.payload.signature")
            .build();
    }
    
    @Test
    @DisplayName("Should validate DPoP token successfully with all security checks")
    void shouldValidateDPoPTokenSuccessfullyWithAllSecurityChecks() {
        // Given
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // Mock successful validations
        when(nonceStore.isValidNonce("test-nonce-456")).thenReturn(true);
        when(nonceStore.isTokenIdUsed("test-token-id-123")).thenReturn(false);
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(true);
        when(jwkValidationClient.calculateJWKThumbprint(validJWK)).thenReturn("example-thumbprint");
        
        // When & Then
        assertThatNoException().isThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, httpUri));
        
        // Verify all validation steps were called
        verify(jwkValidationClient).validateJWK(validJWK);
        verify(jwkValidationClient).verifySignature(validDPoPToken.getRawToken(), validJWK);
        verify(jwkValidationClient).calculateJWKThumbprint(validJWK);
        verify(nonceStore).isValidNonce("test-nonce-456");
        verify(nonceStore).markNonceAsUsed("test-nonce-456");
        verify(nonceStore).isTokenIdUsed("test-token-id-123");
        verify(nonceStore).storeTokenId(eq("test-token-id-123"), any(Instant.class));
    }
    
    @Test
    @DisplayName("Should fail validation when HTTP method does not match")
    void shouldFailValidationWhenHttpMethodDoesNotMatch() {
        // Given
        String accessToken = "example-access-token";
        String wrongHttpMethod = "GET"; // Token expects POST
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, wrongHttpMethod, httpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
        
        // Verify no further validation steps were attempted
        verify(jwkValidationClient, never()).validateJWK(any());
        verify(nonceStore, never()).isValidNonce(any());
    }
    
    @Test
    @DisplayName("Should fail validation when HTTP URI does not match")
    void shouldFailValidationWhenHttpUriDoesNotMatch() {
        // Given
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String wrongHttpUri = "https://amanahfi.ae/api/v1/islamic-finance/ijarah"; // Token expects murabaha
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, wrongHttpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
    }
    
    @Test
    @DisplayName("Should fail validation when access token hash does not match")
    void shouldFailValidationWhenAccessTokenHashDoesNotMatch() {
        // Given
        String wrongAccessToken = "wrong-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, wrongAccessToken, httpMethod, httpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
    }
    
    @Test
    @DisplayName("Should fail validation when JWK validation fails")
    void shouldFailValidationWhenJWKValidationFails() {
        // Given
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // Mock JWK validation failure
        doThrow(new RuntimeException("Invalid JWK")).when(jwkValidationClient).validateJWK(validJWK);
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, httpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
        
        verify(jwkValidationClient).validateJWK(validJWK);
    }
    
    @Test
    @DisplayName("Should fail validation when signature verification fails")
    void shouldFailValidationWhenSignatureVerificationFails() {
        // Given
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // Mock signature verification failure
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(false);
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, httpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
        
        verify(jwkValidationClient).validateJWK(validJWK);
        verify(jwkValidationClient).verifySignature(validDPoPToken.getRawToken(), validJWK);
    }
    
    @Test
    @DisplayName("Should fail validation when JWK thumbprint does not match")
    void shouldFailValidationWhenJWKThumbprintDoesNotMatch() {
        // Given
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // Mock thumbprint mismatch
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(true);
        when(jwkValidationClient.calculateJWKThumbprint(validJWK)).thenReturn("different-thumbprint");
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, httpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
        
        verify(jwkValidationClient).calculateJWKThumbprint(validJWK);
    }
    
    @Test
    @DisplayName("Should fail validation when nonce is invalid or reused")
    void shouldFailValidationWhenNonceIsInvalidOrReused() {
        // Given
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // Mock nonce validation failure
        when(nonceStore.isValidNonce("test-nonce-456")).thenReturn(false);
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(true);
        when(jwkValidationClient.calculateJWKThumbprint(validJWK)).thenReturn("example-thumbprint");
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, httpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
        
        verify(nonceStore).isValidNonce("test-nonce-456");
        verify(nonceStore, never()).markNonceAsUsed(any());
    }
    
    @Test
    @DisplayName("Should fail validation when token ID is already used (replay attack)")
    void shouldFailValidationWhenTokenIdIsAlreadyUsed() {
        // Given
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // Mock token replay detection
        when(nonceStore.isValidNonce("test-nonce-456")).thenReturn(true);
        when(nonceStore.isTokenIdUsed("test-token-id-123")).thenReturn(true); // Token already used
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(true);
        when(jwkValidationClient.calculateJWKThumbprint(validJWK)).thenReturn("example-thumbprint");
        
        // When & Then
        assertThatThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, httpUri))
            .isInstanceOf(DPoPValidationService.DPoPValidationException.class)
            .hasMessageContaining("DPoP token validation failed");
        
        verify(nonceStore).isTokenIdUsed("test-token-id-123");
        verify(nonceStore, never()).storeTokenId(any(), any());
    }
    
    @Test
    @DisplayName("Should handle validation without nonce gracefully")
    void shouldHandleValidationWithoutNonceGracefully() {
        // Given - token without nonce
        Map<String, Object> payloadWithoutNonce = new HashMap<>();
        payloadWithoutNonce.put("jti", "test-token-id-123");
        payloadWithoutNonce.put("htm", "POST");
        payloadWithoutNonce.put("htu", "https://amanahfi.ae/api/v1/islamic-finance/murabaha");
        payloadWithoutNonce.put("iat", Instant.now().getEpochSecond());
        payloadWithoutNonce.put("exp", Instant.now().plusSeconds(300).getEpochSecond());
        
        DPoPToken tokenWithoutNonce = validDPoPToken.toBuilder()
            .payload(payloadWithoutNonce)
            .build();
        
        String accessToken = "example-access-token";
        String httpMethod = "POST";
        String httpUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // Mock successful validations
        when(nonceStore.isTokenIdUsed("test-token-id-123")).thenReturn(false);
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(true);
        when(jwkValidationClient.calculateJWKThumbprint(validJWK)).thenReturn("example-thumbprint");
        
        // When & Then
        assertThatNoException().isThrownBy(() -> 
            dpopValidationService.validateDPoPToken(tokenWithoutNonce, accessToken, httpMethod, httpUri));
        
        // Verify nonce validation was skipped
        verify(nonceStore, never()).isValidNonce(any());
        verify(nonceStore, never()).markNonceAsUsed(any());
    }
    
    @Test
    @DisplayName("Should generate DPoP nonce correctly")
    void shouldGenerateDPoPNonceCorrectly() {
        // Given
        String expectedNonce = "generated-nonce-123";
        when(nonceStore.generateNonce()).thenReturn(expectedNonce);
        
        // When
        String actualNonce = dpopValidationService.generateDPoPNonce();
        
        // Then
        assertThat(actualNonce).isEqualTo(expectedNonce);
        verify(nonceStore).generateNonce();
    }
    
    @Test
    @DisplayName("Should determine DPoP requirement for high security endpoints")
    void shouldDetermineDPoPRequirementForHighSecurityEndpoints() {
        // Given - high security endpoints
        String adminEndpoint = "https://amanahfi.ae/api/v1/admin/users";
        String complianceEndpoint = "https://amanahfi.ae/api/v1/compliance/reports";
        String regulatoryEndpoint = "https://amanahfi.ae/api/v1/regulatory/submissions";
        String cbdcEndpoint = "https://amanahfi.ae/api/v1/cbdc/digital-dirham";
        
        // When & Then
        assertThat(dpopValidationService.isDPoPRequired("GET", adminEndpoint, "client-123")).isTrue();
        assertThat(dpopValidationService.isDPoPRequired("POST", complianceEndpoint, "client-123")).isTrue();
        assertThat(dpopValidationService.isDPoPRequired("PUT", regulatoryEndpoint, "client-123")).isTrue();
        assertThat(dpopValidationService.isDPoPRequired("POST", cbdcEndpoint, "client-123")).isTrue();
    }
    
    @Test
    @DisplayName("Should determine DPoP requirement for payment endpoints")
    void shouldDetermineDPoPRequirementForPaymentEndpoints() {
        // Given - payment and transfer endpoints
        String paymentEndpoint = "https://amanahfi.ae/api/v1/payments/transfer";
        String transferEndpoint = "https://amanahfi.ae/api/v1/transfers/international";
        String islamicFinanceEndpoint = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        
        // When & Then
        assertThat(dpopValidationService.isDPoPRequired("POST", paymentEndpoint, "client-123")).isTrue();
        assertThat(dpopValidationService.isDPoPRequired("POST", transferEndpoint, "client-123")).isTrue();
        assertThat(dpopValidationService.isDPoPRequired("POST", islamicFinanceEndpoint, "client-123")).isTrue();
    }
    
    @Test
    @DisplayName("Should not require DPoP for regular endpoints")
    void shouldNotRequireDPoPForRegularEndpoints() {
        // Given - regular endpoints
        String userEndpoint = "https://amanahfi.ae/api/v1/users/profile";
        String publicEndpoint = "https://amanahfi.ae/api/v1/public/rates";
        
        // When & Then
        assertThat(dpopValidationService.isDPoPRequired("GET", userEndpoint, "client-123")).isFalse();
        assertThat(dpopValidationService.isDPoPRequired("GET", publicEndpoint, "client-123")).isFalse();
    }
    
    @Test
    @DisplayName("Should create DPoP error response correctly")
    void shouldCreateDPoPErrorResponseCorrectly() {
        // Given
        String error = "invalid_dpop_proof";
        String description = "The DPoP proof is invalid";
        String nonce = "challenge-nonce-789";
        
        // When
        DPoPValidationService.DPoPErrorResponse errorResponse = 
            dpopValidationService.createErrorResponse(error, description, nonce);
        
        // Then
        assertThat(errorResponse.getError()).isEqualTo(error);
        assertThat(errorResponse.getErrorDescription()).isEqualTo(description);
        assertThat(errorResponse.getNonce()).isEqualTo(nonce);
    }
    
    @Test
    @DisplayName("Should validate Islamic finance operations with enhanced security")
    void shouldValidateIslamicFinanceOperationsWithEnhancedSecurity() {
        // Given - Islamic finance DPoP token
        String islamicFinanceUri = "https://amanahfi.ae/api/v1/islamic-finance/murabaha";
        String accessToken = "islamic-finance-token";
        String httpMethod = "POST";
        
        // Mock successful validations
        when(nonceStore.isValidNonce("test-nonce-456")).thenReturn(true);
        when(nonceStore.isTokenIdUsed("test-token-id-123")).thenReturn(false);
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(true);
        when(jwkValidationClient.calculateJWKThumbprint(validJWK)).thenReturn("example-thumbprint");
        
        // When & Then
        assertThatNoException().isThrownBy(() -> 
            dpopValidationService.validateDPoPToken(validDPoPToken, accessToken, httpMethod, islamicFinanceUri));
        
        // Verify enhanced security for Islamic finance
        assertThat(dpopValidationService.isDPoPRequired(httpMethod, islamicFinanceUri, "client-123")).isTrue();
    }
    
    @Test
    @DisplayName("Should validate CBDC operations with maximum security")
    void shouldValidateCBDCOperationsWithMaximumSecurity() {
        // Given - CBDC DPoP token
        String cbdcUri = "https://amanahfi.ae/api/v1/cbdc/digital-dirham/transfer";
        
        Map<String, Object> cbdcPayload = new HashMap<>();
        cbdcPayload.put("jti", "cbdc-token-id-789");
        cbdcPayload.put("htm", "POST");
        cbdcPayload.put("htu", cbdcUri);
        cbdcPayload.put("iat", Instant.now().getEpochSecond());
        cbdcPayload.put("exp", Instant.now().plusSeconds(300).getEpochSecond());
        cbdcPayload.put("nonce", "cbdc-nonce-456");
        
        DPoPToken cbdcToken = validDPoPToken.toBuilder()
            .payload(cbdcPayload)
            .httpUri(cbdcUri)
            .build();
        
        String accessToken = "cbdc-access-token";
        String httpMethod = "POST";
        
        // Mock successful validations
        when(nonceStore.isValidNonce("cbdc-nonce-456")).thenReturn(true);
        when(nonceStore.isTokenIdUsed("cbdc-token-id-789")).thenReturn(false);
        when(jwkValidationClient.verifySignature(any(), any())).thenReturn(true);
        when(jwkValidationClient.calculateJWKThumbprint(validJWK)).thenReturn("example-thumbprint");
        
        // When & Then
        assertThatNoException().isThrownBy(() -> 
            dpopValidationService.validateDPoPToken(cbdcToken, accessToken, httpMethod, cbdcUri));
        
        // Verify maximum security for CBDC operations
        assertThat(dpopValidationService.isDPoPRequired(httpMethod, cbdcUri, "client-123")).isTrue();
    }
}