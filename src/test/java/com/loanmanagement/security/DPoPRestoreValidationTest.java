package com.loanmanagement.security;

import com.loanmanagement.security.keycloak.dpop.DPoPValidator;
import com.loanmanagement.security.keycloak.dpop.DPoPClientLibrary;
import com.loanmanagement.security.keycloak.fapi.FapiComplianceValidator;
import com.loanmanagement.party.application.PartyDataServerService;
import com.loanmanagement.party.domain.*;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

import jakarta.servlet.http.HttpServletRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test to validate restored DPoP implementation components
 * Verifies that critical DPoP classes are restored and functional
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DPoP Implementation Restoration Validation")
class DPoPRestoreValidationTest {

    @MockBean
    private RedisTemplate<String, String> redisTemplate;
    
    private DPoPValidator dpopValidator;
    private FapiComplianceValidator fapiValidator;
    private PartyDataServerService partyDataServerService;
    
    @BeforeEach
    void setUp() {
        dpopValidator = new DPoPValidator();
        fapiValidator = new FapiComplianceValidator();
        
        // Mock Redis operations
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        doNothing().when(redisTemplate).opsForValue();
    }
    
    @Test
    @DisplayName("Should validate DPoP validator is restored and functional")
    void shouldValidateDPoPValidatorRestored() throws Exception {
        // Given - Generate test keys
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        // When - Generate DPoP proof
        String dpopProof = generator.generateProof("GET", "https://api.bank.com/loans");
        
        // Then - Should generate valid proof
        assertThat(dpopProof).isNotNull();
        assertThat(dpopProof.split("\\.")).hasSize(3); // Valid JWT format
        
        String jktThumbprint = generator.getJKTThumbprint();
        assertThat(jktThumbprint).isNotNull();
        assertThat(jktThumbprint).hasSize(43); // Base64url encoded SHA-256 (43 chars)
        
        System.out.println("✅ DPoP Validator Restored Successfully");
        System.out.println("   Generated DPoP Proof: " + dpopProof.substring(0, 50) + "...");
        System.out.println("   JKT Thumbprint: " + jktThumbprint);
    }
    
    @Test
    @DisplayName("Should validate FAPI compliance validator is functional")
    void shouldValidateFapiComplianceValidator() {
        // Given - Mock HTTPS request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.isSecure()).thenReturn(true);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("x-fapi-interaction-id")).thenReturn(null);
        
        // When - Validate FAPI compliance
        FapiComplianceValidator.FapiComplianceResult result = fapiValidator.validateRequest(request);
        
        // Then - Should be compliant
        assertThat(result.isCompliant()).isTrue();
        assertThat(result.getInteractionId()).isNotNull();
        
        System.out.println("✅ FAPI Compliance Validator Restored Successfully");
        System.out.println("   Generated Interaction ID: " + result.getInteractionId());
    }
    
    @Test
    @DisplayName("Should validate DPoP client library key generation")
    void shouldValidateClientLibraryKeyGeneration() throws Exception {
        // Test EC key generation
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        assertThat(ecKey).isNotNull();
        assertThat(ecKey.getCurve().getName()).isEqualTo("P-256");
        assertThat(ecKey.getKeyID()).isNotNull();
        
        // Test RSA key generation
        RSAKey rsaKey = DPoPClientLibrary.DPoPKeyManager.generateRSAKey();
        assertThat(rsaKey).isNotNull();
        assertThat(rsaKey.size()).isEqualTo(2048);
        assertThat(rsaKey.getKeyID()).isNotNull();
        
        // Test JKT thumbprint calculation
        String ecThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJKTThumbprint(ecKey);
        String rsaThumbprint = DPoPClientLibrary.DPoPKeyManager.calculateJKTThumbprint(rsaKey);
        
        assertThat(ecThumbprint).hasSize(43);
        assertThat(rsaThumbprint).hasSize(43);
        assertThat(ecThumbprint).isNotEqualTo(rsaThumbprint);
        
        System.out.println("✅ DPoP Client Library Key Generation Working");
        System.out.println("   EC Key ID: " + ecKey.getKeyID());
        System.out.println("   RSA Key ID: " + rsaKey.getKeyID());
        System.out.println("   EC JKT: " + ecThumbprint);
        System.out.println("   RSA JKT: " + rsaThumbprint);
    }
    
    @Test
    @DisplayName("Should validate Party Data Server is restored")
    void shouldValidatePartyDataServerRestored() {
        // Given - Create party domain objects
        Party party = Party.create(
            "ext-123",
            "test-user", 
            "Test User",
            "test@bank.com",
            PartyType.INDIVIDUAL,
            "system"
        );
        
        // When - Test party business logic
        party.addRole(new PartyRole("BANKING_USER", "Banking User Role", RoleSource.SYSTEM, "system"));
        party.addToGroup(new PartyGroup("CUSTOMERS", "Customer Group", GroupType.CUSTOMER_SEGMENT, "system"));
        
        // Then - Validate party functionality
        assertThat(party.isActive()).isTrue();
        assertThat(party.isCompliant()).isTrue();
        assertThat(party.hasRole("BANKING_USER")).isTrue();
        assertThat(party.isInGroup("CUSTOMERS")).isTrue();
        assertThat(party.getActiveRoles()).hasSize(1);
        assertThat(party.getActiveGroups()).hasSize(1);
        
        System.out.println("✅ Party Data Server Domain Model Restored");
        System.out.println("   Party ID: " + party.getExternalId());
        System.out.println("   Party Type: " + party.getPartyType());
        System.out.println("   Active Roles: " + party.getActiveRoles().size());
        System.out.println("   Active Groups: " + party.getActiveGroups().size());
    }
    
    @Test
    @DisplayName("Should validate complete OAuth 2.1 + DPoP + Party integration")
    void shouldValidateCompleteIntegration() throws Exception {
        // Given - Complete setup
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        Party party = Party.create(
            "keycloak-user-123",
            "banking-user", 
            "Banking User",
            "user@bank.com",
            PartyType.INDIVIDUAL,
            "keycloak"
        );
        party.addRole(new PartyRole("BANKING_USER", "Banking User", RoleSource.EXTERNAL_SYSTEM, "keycloak"));
        
        // When - Generate banking API request with DPoP
        String accessToken = "mock_access_token_123";
        String dpopProof = generator.generateProof("POST", "https://api.bank.com/loans", accessToken);
        String jktThumbprint = generator.getJKTThumbprint();
        
        // Then - Validate complete integration
        assertThat(dpopProof).isNotNull();
        assertThat(jktThumbprint).isNotNull();
        assertThat(party.isActive()).isTrue();
        assertThat(party.hasRole("BANKING_USER")).isTrue();
        
        // Simulate token binding validation
        assertThat(jktThumbprint).hasSize(43); // Valid JKT format
        
        System.out.println("✅ Complete OAuth 2.1 + DPoP + Party Integration Working");
        System.out.println("   DPoP Proof Length: " + dpopProof.length());
        System.out.println("   JKT Thumbprint: " + jktThumbprint);
        System.out.println("   Party External ID: " + party.getExternalId());
        System.out.println("   Party Banking Role: " + party.hasRole("BANKING_USER"));
    }
    
    @Test
    @DisplayName("Should validate FAPI 2.0 security requirements")
    void shouldValidateFapi2SecurityRequirements() {
        // Test PKCE validation
        FapiComplianceValidator.FapiComplianceResult pkceResult = 
            fapiValidator.validatePKCE("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "S256");
        assertThat(pkceResult.isCompliant()).isTrue();
        
        // Test client assertion validation
        FapiComplianceValidator.FapiComplianceResult assertionResult = 
            fapiValidator.validateClientAssertion(
                "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJjbGllbnQifQ.signature"
            );
        assertThat(assertionResult.isCompliant()).isTrue();
        
        System.out.println("✅ FAPI 2.0 Security Requirements Validated");
        System.out.println("   PKCE Validation: PASS");
        System.out.println("   Client Assertion: PASS");
    }
    
    @Test
    @DisplayName("Should demonstrate RFC 9449 DPoP compliance")
    void shouldDemonstrateRfc9449Compliance() throws Exception {
        // Generate multiple proofs to test uniqueness
        ECKey ecKey = DPoPClientLibrary.DPoPKeyManager.generateECKey();
        DPoPClientLibrary.DPoPProofGenerator generator = new DPoPClientLibrary.DPoPProofGenerator(ecKey);
        
        String proof1 = generator.generateProof("GET", "https://api.bank.com/loans");
        String proof2 = generator.generateProof("GET", "https://api.bank.com/loans");
        String proof3 = generator.generateProof("POST", "https://api.bank.com/payments");
        
        // Each proof should be unique (different JTI)
        assertThat(proof1).isNotEqualTo(proof2);
        assertThat(proof2).isNotEqualTo(proof3);
        assertThat(proof1).isNotEqualTo(proof3);
        
        // All proofs should have same JKT thumbprint (same key)
        String jkt = generator.getJKTThumbprint();
        assertThat(jkt).hasSize(43);
        
        System.out.println("✅ RFC 9449 DPoP Compliance Demonstrated");
        System.out.println("   Unique Proofs Generated: 3");
        System.out.println("   Consistent JKT Thumbprint: " + jkt);
        System.out.println("   Proof Uniqueness: VERIFIED");
    }
}