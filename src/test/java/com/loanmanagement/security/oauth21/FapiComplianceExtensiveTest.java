package com.loanmanagement.security.oauth21;

import com.loanmanagement.security.keycloak.fapi.FapiComplianceValidator;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Extensive FAPI 2.0 Compliance Validation Test Suite
 * Tests all FAPI security requirements and validation scenarios
 * Contributes to 15% of total OAuth 2.1 + DPoP coverage
 */
@DisplayName("FAPI 2.0 Compliance - Extensive Validation Tests")
class FapiComplianceExtensiveTest {

    private FapiComplianceValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new FapiComplianceValidator();
    }
    
    @Test
    @DisplayName("Should validate HTTPS requirements with different header configurations")
    void shouldValidateHTTPSRequirementsWithDifferentHeaders() {
        // Test direct HTTPS
        MockHttpServletRequest httpsRequest = new MockHttpServletRequest();
        httpsRequest.setSecure(true);
        httpsRequest.setMethod("GET");
        
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validateRequest(httpsRequest);
        assertThat(result1.isCompliant()).isTrue();
        
        // Test x-forwarded-proto header
        MockHttpServletRequest forwardedRequest = new MockHttpServletRequest();
        forwardedRequest.setSecure(false);
        forwardedRequest.addHeader("x-forwarded-proto", "https");
        forwardedRequest.setMethod("POST");
        
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validateRequest(forwardedRequest);
        assertThat(result2.isCompliant()).isTrue();
        
        // Test x-forwarded-ssl header
        MockHttpServletRequest sslRequest = new MockHttpServletRequest();
        sslRequest.setSecure(false);
        sslRequest.addHeader("x-forwarded-ssl", "on");
        sslRequest.setMethod("PUT");
        
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validateRequest(sslRequest);
        assertThat(result3.isCompliant()).isTrue();
        
        // Test non-HTTPS request
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setSecure(false);
        httpRequest.setMethod("GET");
        
        FapiComplianceValidator.FapiComplianceResult result4 = validator.validateRequest(httpRequest);
        assertThat(result4.isCompliant()).isFalse();
        assertThat(result4.getViolationMessage()).contains("HTTPS");
    }
    
    @Test
    @DisplayName("Should validate FAPI interaction ID header requirements")
    void shouldValidateFapiInteractionIdHeaderRequirements() {
        // Test valid UUID interaction ID
        MockHttpServletRequest validRequest = new MockHttpServletRequest();
        validRequest.setSecure(true);
        validRequest.setMethod("GET");
        validRequest.addHeader("x-fapi-interaction-id", "550e8400-e29b-41d4-a716-446655440000");
        
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validateRequest(validRequest);
        assertThat(result1.isCompliant()).isTrue();
        assertThat(result1.getInteractionId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        
        // Test missing interaction ID (should generate one)
        MockHttpServletRequest missingIdRequest = new MockHttpServletRequest();
        missingIdRequest.setSecure(true);
        missingIdRequest.setMethod("POST");
        
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validateRequest(missingIdRequest);
        assertThat(result2.isCompliant()).isTrue();
        assertThat(result2.getInteractionId()).isNotNull();
        assertThat(result2.getInteractionId()).matches("[a-f0-9-]{36}"); // UUID format
        
        // Test invalid UUID format
        MockHttpServletRequest invalidIdRequest = new MockHttpServletRequest();
        invalidIdRequest.setSecure(true);
        invalidIdRequest.setMethod("GET");
        invalidIdRequest.addHeader("x-fapi-interaction-id", "invalid-uuid-format");
        
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validateRequest(invalidIdRequest);
        assertThat(result3.isCompliant()).isTrue(); // Should generate new ID
        assertThat(result3.getInteractionId()).matches("[a-f0-9-]{36}");
    }
    
    @Test
    @DisplayName("Should validate FAPI auth date header format")
    void shouldValidateFapiAuthDateHeaderFormat() {
        // Test valid RFC 3339 format
        MockHttpServletRequest validDateRequest = new MockHttpServletRequest();
        validDateRequest.setSecure(true);
        validDateRequest.setMethod("GET");
        validDateRequest.addHeader("x-fapi-auth-date", "2024-01-15T10:30:00Z");
        
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validateRequest(validDateRequest);
        assertThat(result1.isCompliant()).isTrue();
        
        // Test valid RFC 3339 with timezone
        MockHttpServletRequest timezoneRequest = new MockHttpServletRequest();
        timezoneRequest.setSecure(true);
        timezoneRequest.setMethod("POST");
        timezoneRequest.addHeader("x-fapi-auth-date", "2024-01-15T10:30:00+01:00");
        
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validateRequest(timezoneRequest);
        assertThat(result2.isCompliant()).isTrue();
        
        // Test invalid date format
        MockHttpServletRequest invalidDateRequest = new MockHttpServletRequest();
        invalidDateRequest.setSecure(true);
        invalidDateRequest.setMethod("GET");
        invalidDateRequest.addHeader("x-fapi-auth-date", "invalid-date-format");
        
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validateRequest(invalidDateRequest);
        assertThat(result3.isCompliant()).isFalse();
        assertThat(result3.getViolationMessage()).contains("x-fapi-auth-date");
    }
    
    @Test
    @DisplayName("Should validate customer IP address header format")
    void shouldValidateCustomerIpAddressHeaderFormat() {
        // Test valid IPv4 address
        MockHttpServletRequest ipv4Request = new MockHttpServletRequest();
        ipv4Request.setSecure(true);
        ipv4Request.setMethod("GET");
        ipv4Request.addHeader("x-fapi-customer-ip-address", "192.168.1.100");
        
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validateRequest(ipv4Request);
        assertThat(result1.isCompliant()).isTrue();
        
        // Test valid IPv6 address
        MockHttpServletRequest ipv6Request = new MockHttpServletRequest();
        ipv6Request.setSecure(true);
        ipv6Request.setMethod("POST");
        ipv6Request.addHeader("x-fapi-customer-ip-address", "2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validateRequest(ipv6Request);
        assertThat(result2.isCompliant()).isTrue();
        
        // Test invalid IP format
        MockHttpServletRequest invalidIpRequest = new MockHttpServletRequest();
        invalidIpRequest.setSecure(true);
        invalidIpRequest.setMethod("GET");
        invalidIpRequest.addHeader("x-fapi-customer-ip-address", "invalid.ip.address");
        
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validateRequest(invalidIpRequest);
        assertThat(result3.isCompliant()).isFalse();
        assertThat(result3.getViolationMessage()).contains("ip-address");
    }
    
    @Test
    @DisplayName("Should reject dangerous HTTP methods")
    void shouldRejectDangerousHTTPMethods() {
        String[] dangerousMethods = {"TRACE", "TRACK"};
        
        for (String method : dangerousMethods) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setSecure(true);
            request.setMethod(method);
            
            FapiComplianceValidator.FapiComplianceResult result = validator.validateRequest(request);
            assertThat(result.isCompliant()).isFalse();
            assertThat(result.getViolationMessage()).contains("not allowed");
        }
    }
    
    @Test
    @DisplayName("Should validate PKCE parameters comprehensively")
    void shouldValidatePKCEParametersComprehensively() {
        // Valid PKCE with S256
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validatePKCE(
            "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "S256"
        );
        assertThat(result1.isCompliant()).isTrue();
        
        // Valid PKCE with different challenge
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validatePKCE(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", "S256"
        );
        assertThat(result2.isCompliant()).isTrue();
        
        // Invalid method - plain (FAPI requires S256)
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validatePKCE(
            "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk", "plain"
        );
        assertThat(result3.isCompliant()).isFalse();
        assertThat(result3.getViolationMessage()).contains("S256");
        
        // Missing challenge
        FapiComplianceValidator.FapiComplianceResult result4 = validator.validatePKCE(null, "S256");
        assertThat(result4.isCompliant()).isFalse();
        assertThat(result4.getViolationMessage()).contains("code_challenge");
        
        // Empty challenge
        FapiComplianceValidator.FapiComplianceResult result5 = validator.validatePKCE("", "S256");
        assertThat(result5.isCompliant()).isFalse();
        
        // Invalid challenge format (not Base64url)
        FapiComplianceValidator.FapiComplianceResult result6 = validator.validatePKCE(
            "invalid+challenge/with=padding", "S256"
        );
        assertThat(result6.isCompliant()).isFalse();
        assertThat(result6.getViolationMessage()).contains("format");
    }
    
    @Test
    @DisplayName("Should validate client assertion requirements")
    void shouldValidateClientAssertionRequirements() {
        // Valid client assertion
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validateClientAssertion(
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjbGllbnQtaWQiLCJzdWIiOiJjbGllbnQtaWQiLCJhdWQiOiJodHRwczovL2F1dGguc2VydmVyLmNvbS90b2tlbiIsImV4cCI6MTY0MDk5NTIwMCwiaWF0IjoxNjQwOTk0OTAwLCJqdGkiOiJ1bmlxdWUtaWQifQ.signature"
        );
        assertThat(result1.isCompliant()).isTrue();
        
        // Invalid assertion type
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validateClientAssertion(
            "client_secret_basic",
            "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJjbGllbnQifQ.signature"
        );
        assertThat(result2.isCompliant()).isFalse();
        assertThat(result2.getViolationMessage()).contains("private_key_jwt");
        
        // Missing client assertion
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validateClientAssertion(
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
            null
        );
        assertThat(result3.isCompliant()).isFalse();
        assertThat(result3.getViolationMessage()).contains("required");
        
        // Empty client assertion
        FapiComplianceValidator.FapiComplianceResult result4 = validator.validateClientAssertion(
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
            ""
        );
        assertThat(result4.isCompliant()).isFalse();
        
        // Invalid JWT format
        FapiComplianceValidator.FapiComplianceResult result5 = validator.validateClientAssertion(
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
            "not.a.valid.jwt.format"
        );
        assertThat(result5.isCompliant()).isFalse();
        assertThat(result5.getViolationMessage()).contains("JWT format");
    }
    
    @Test
    @DisplayName("Should validate token lifetime according to FAPI requirements")
    void shouldValidateTokenLifetimeAccordingToFapi() {
        Instant now = Instant.now();
        
        // Valid token lifetime (30 minutes)
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validateTokenLifetime(
            now, now.plus(30, ChronoUnit.MINUTES)
        );
        assertThat(result1.isCompliant()).isTrue();
        
        // Valid token lifetime (1 hour - maximum)
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validateTokenLifetime(
            now, now.plus(1, ChronoUnit.HOURS)
        );
        assertThat(result2.isCompliant()).isTrue();
        
        // Invalid token lifetime (2 hours - exceeds FAPI limit)
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validateTokenLifetime(
            now, now.plus(2, ChronoUnit.HOURS)
        );
        assertThat(result3.isCompliant()).isFalse();
        assertThat(result3.getViolationMessage()).contains("lifetime");
        assertThat(result3.getViolationMessage()).contains("exceeds");
        
        // Null timestamps
        FapiComplianceValidator.FapiComplianceResult result4 = validator.validateTokenLifetime(null, null);
        assertThat(result4.isCompliant()).isFalse();
        assertThat(result4.getViolationMessage()).contains("valid issued and expiry times");
    }
    
    @Test
    @DisplayName("Should validate authentication age requirements")
    void shouldValidateAuthenticationAgeRequirements() {
        Instant now = Instant.now();
        
        // Recent authentication (30 minutes ago)
        FapiComplianceValidator.FapiComplianceResult result1 = validator.validateAuthAge(
            now.minus(30, ChronoUnit.MINUTES)
        );
        assertThat(result1.isCompliant()).isTrue();
        
        // Authentication at FAPI limit (2 hours)
        FapiComplianceValidator.FapiComplianceResult result2 = validator.validateAuthAge(
            now.minus(2, ChronoUnit.HOURS)
        );
        assertThat(result2.isCompliant()).isTrue();
        
        // Old authentication (3 hours - exceeds limit)
        FapiComplianceValidator.FapiComplianceResult result3 = validator.validateAuthAge(
            now.minus(3, ChronoUnit.HOURS)
        );
        assertThat(result3.isCompliant()).isFalse();
        assertThat(result3.getViolationMessage()).contains("too old");
        
        // Null authentication time (should be compliant - not required for all flows)
        FapiComplianceValidator.FapiComplianceResult result4 = validator.validateAuthAge(null);
        assertThat(result4.isCompliant()).isTrue();
    }
    
    @Test
    @DisplayName("Should handle edge cases in validation")
    void shouldHandleEdgeCasesInValidation() {
        // Test with null request
        assertThatThrownBy(() -> validator.validateRequest(null))
            .isInstanceOf(NullPointerException.class);
        
        // Test with minimal valid request
        MockHttpServletRequest minimalRequest = new MockHttpServletRequest();
        minimalRequest.setSecure(true);
        minimalRequest.setMethod("GET");
        
        FapiComplianceValidator.FapiComplianceResult result = validator.validateRequest(minimalRequest);
        assertThat(result.isCompliant()).isTrue();
        assertThat(result.getInteractionId()).isNotNull();
        
        // Test with unusual but valid HTTP methods
        String[] validMethods = {"GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"};
        
        for (String method : validMethods) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setSecure(true);
            request.setMethod(method);
            
            FapiComplianceValidator.FapiComplianceResult methodResult = validator.validateRequest(request);
            assertThat(methodResult.isCompliant()).isTrue();
        }
    }
}