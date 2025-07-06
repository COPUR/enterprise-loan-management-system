package com.bank.loan.loan.api.security;

import com.bank.loan.loan.security.dpop.filter.DPoPValidationFilter;
import com.bank.loan.loan.security.dpop.service.DPoPTokenValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPNonceService;
import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
import com.bank.loan.loan.security.dpop.util.DPoPTestKeyGenerator;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DPoP Security Filter Tests")
class DPoPSecurityFilterTest {

    @Mock
    private DPoPTokenValidationService dpopTokenValidationService;

    @Mock
    private DPoPNonceService dpopNonceService;

    @Mock
    private FilterChain filterChain;

    private DPoPValidationFilter dpopValidationFilter;
    private DPoPTestKeyGenerator keyGenerator;
    private ECKey testKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        dpopValidationFilter = new DPoPValidationFilter(dpopTokenValidationService, dpopNonceService);
        keyGenerator = new DPoPTestKeyGenerator();
        testKeyPair = keyGenerator.generateECKey();
    }

    @Nested
    @DisplayName("Filter Chain Execution Tests")
    class FilterChainExecutionTests {

        @Test
        @DisplayName("Should proceed with filter chain for valid DPoP request")
        void shouldProceedWithFilterChainForValidDPoPRequest() throws Exception {
            MockHttpServletRequest request = createValidDPoPRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            doNothing().when(dpopTokenValidationService).validateDPoPBoundToken(
                    anyString(), anyString(), anyString(), anyString());

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should block filter chain for invalid DPoP request")
        void shouldBlockFilterChainForInvalidDPoPRequest() throws Exception {
            MockHttpServletRequest request = createInvalidDPoPRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            doThrow(new InvalidDPoPProofException("Invalid proof"))
                    .when(dpopTokenValidationService).validateDPoPBoundToken(
                            anyString(), anyString(), anyString(), anyString());

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate")).contains("DPoP");
        }

        @Test
        @DisplayName("Should skip DPoP validation for exempt endpoints")
        void shouldSkipDPoPValidationForExemptEndpoints() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/actuator/health");
            request.setMethod("GET");

            MockHttpServletResponse response = new MockHttpServletResponse();

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(dpopTokenValidationService, never()).validateDPoPBoundToken(
                    anyString(), anyString(), anyString(), anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("DPoP Header Validation Tests")
    class DPoPHeaderValidationTests {

        @Test
        @DisplayName("Should validate presence of DPoP header")
        void shouldValidatePresenceOfDPoPHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/loans");
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer mock.access.token");
            // Missing DPoP header

            MockHttpServletResponse response = new MockHttpServletResponse();

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate")).contains("DPoP");
        }

        @Test
        @DisplayName("Should validate presence of Authorization header")
        void shouldValidatePresenceOfAuthorizationHeader() throws Exception {
            String dpopProof = keyGenerator.createValidDPoPProof(
                    testKeyPair, "GET", "http://localhost/api/v1/loans");

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/loans");
            request.setMethod("GET");
            request.addHeader("DPoP", dpopProof);
            // Missing Authorization header

            MockHttpServletResponse response = new MockHttpServletResponse();

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(filterChain, never()).doFilter(any(), any());
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("Should extract access token from Bearer header")
        void shouldExtractAccessTokenFromBearerHeader() throws Exception {
            String accessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", "http://localhost/api/v1/loans", accessToken);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/loans");
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer " + accessToken);
            request.addHeader("DPoP", dpopProof);

            MockHttpServletResponse response = new MockHttpServletResponse();

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(dpopTokenValidationService).validateDPoPBoundToken(
                    eq(accessToken), eq(dpopProof), eq("GET"), contains("/api/v1/loans"));
        }
    }

    @Nested
    @DisplayName("Error Response Generation Tests")
    class ErrorResponseGenerationTests {

        @Test
        @DisplayName("Should generate proper error response for invalid DPoP proof")
        void shouldGenerateProperErrorResponseForInvalidDPoPProof() throws Exception {
            MockHttpServletRequest request = createValidDPoPRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            doThrow(new InvalidDPoPProofException("Invalid signature"))
                    .when(dpopTokenValidationService).validateDPoPBoundToken(
                            anyString(), anyString(), anyString(), anyString());

            dpopValidationFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate"))
                    .contains("DPoP error=\"invalid_dpop_proof\"");
            assertThat(response.getHeader("WWW-Authenticate"))
                    .contains("error_description=\"Invalid signature\"");
        }

        @Test
        @DisplayName("Should generate nonce challenge when required")
        void shouldGenerateNonceChallengeWhenRequired() throws Exception {
            MockHttpServletRequest request = createValidDPoPRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            String nonce = "test_nonce_123";
            when(dpopNonceService.generateNonce()).thenReturn(nonce);
            when(dpopNonceService.isNonceRequired(anyString())).thenReturn(true);

            doThrow(new InvalidDPoPProofException("Nonce required"))
                    .when(dpopTokenValidationService).validateDPoPBoundToken(
                            anyString(), anyString(), anyString(), anyString());

            dpopValidationFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getHeader("WWW-Authenticate"))
                    .contains("DPoP error=\"use_dpop_nonce\"");
            assertThat(response.getHeader("DPoP-Nonce")).isEqualTo(nonce);
        }

        @Test
        @DisplayName("Should include CORS headers in error responses")
        void shouldIncludeCorsHeadersInErrorResponses() throws Exception {
            MockHttpServletRequest request = createValidDPoPRequest();
            request.addHeader("Origin", "https://example.com");
            MockHttpServletResponse response = new MockHttpServletResponse();

            doThrow(new InvalidDPoPProofException("Invalid proof"))
                    .when(dpopTokenValidationService).validateDPoPBoundToken(
                            anyString(), anyString(), anyString(), anyString());

            dpopValidationFilter.doFilter(request, response, filterChain);

            assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://example.com");
            assertThat(response.getHeader("Access-Control-Expose-Headers"))
                    .contains("WWW-Authenticate", "DPoP-Nonce");
        }
    }

    @Nested
    @DisplayName("Request URL Construction Tests")
    class RequestUrlConstructionTests {

        @Test
        @DisplayName("Should construct correct HTTP URL for validation")
        void shouldConstructCorrectHttpUrlForValidation() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(8080);
            request.setRequestURI("/api/v1/loans");
            request.setQueryString("page=1&size=10");
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer mock.access.token");
            request.addHeader("DPoP", "mock.dpop.proof");

            MockHttpServletResponse response = new MockHttpServletResponse();

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(dpopTokenValidationService).validateDPoPBoundToken(
                    anyString(), anyString(), eq("GET"), 
                    eq("http://localhost:8080/api/v1/loans?page=1&size=10"));
        }

        @Test
        @DisplayName("Should construct correct HTTPS URL for validation")
        void shouldConstructCorrectHttpsUrlForValidation() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setScheme("https");
            request.setServerName("api.example.com");
            request.setServerPort(443);
            request.setRequestURI("/api/v1/loans");
            request.setMethod("GET");
            request.addHeader("Authorization", "Bearer mock.access.token");
            request.addHeader("DPoP", "mock.dpop.proof");

            MockHttpServletResponse response = new MockHttpServletResponse();

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(dpopTokenValidationService).validateDPoPBoundToken(
                    anyString(), anyString(), eq("GET"), eq("https://api.example.com/api/v1/loans"));
        }

        @Test
        @DisplayName("Should handle X-Forwarded headers for URL construction")
        void shouldHandleXForwardedHeadersForUrlConstruction() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setScheme("http");
            request.setServerName("localhost");
            request.setServerPort(8080);
            request.setRequestURI("/api/v1/loans");
            request.setMethod("GET");
            request.addHeader("X-Forwarded-Proto", "https");
            request.addHeader("X-Forwarded-Host", "api.example.com");
            request.addHeader("X-Forwarded-Port", "443");
            request.addHeader("Authorization", "Bearer mock.access.token");
            request.addHeader("DPoP", "mock.dpop.proof");

            MockHttpServletResponse response = new MockHttpServletResponse();

            dpopValidationFilter.doFilter(request, response, filterChain);

            verify(dpopTokenValidationService).validateDPoPBoundToken(
                    anyString(), anyString(), eq("GET"), eq("https://api.example.com/api/v1/loans"));
        }
    }

    @Nested
    @DisplayName("Filter Configuration Tests")
    class FilterConfigurationTests {

        @Test
        @DisplayName("Should respect DPoP-exempt endpoint configuration")
        void shouldRespectDPoPExemptEndpointConfiguration() throws Exception {
            String[] exemptEndpoints = {"/oauth2/token", "/actuator/health", "/api/public/*"};

            for (String endpoint : exemptEndpoints) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setRequestURI(endpoint);
                request.setMethod("GET");

                MockHttpServletResponse response = new MockHttpServletResponse();

                dpopValidationFilter.doFilter(request, response, filterChain);

                verify(filterChain).doFilter(request, response);
                verifyNoInteractions(dpopTokenValidationService);

                reset(filterChain);
            }
        }

        @Test
        @DisplayName("Should apply DPoP validation to protected endpoints")
        void shouldApplyDPoPValidationToProtectedEndpoints() throws Exception {
            String[] protectedEndpoints = {
                    "/api/v1/loans", "/api/v1/payments", "/api/v1/customers", "/api/v1/fapi/loans"
            };

            for (String endpoint : protectedEndpoints) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setRequestURI(endpoint);
                request.setMethod("GET");
                request.addHeader("Authorization", "Bearer mock.access.token");
                // Missing DPoP header

                MockHttpServletResponse response = new MockHttpServletResponse();

                dpopValidationFilter.doFilter(request, response, filterChain);

                assertThat(response.getStatus()).isEqualTo(401);
                verify(filterChain, never()).doFilter(request, response);

                reset(filterChain);
            }
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should process DPoP validation efficiently")
        void shouldProcessDPoPValidationEfficiently() throws Exception {
            MockHttpServletRequest request = createValidDPoPRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            doNothing().when(dpopTokenValidationService).validateDPoPBoundToken(
                    anyString(), anyString(), anyString(), anyString());

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 1000; i++) {
                dpopValidationFilter.doFilter(request, response, filterChain);
            }

            long endTime = System.currentTimeMillis();
            long averageTime = (endTime - startTime) / 1000;

            assertThat(averageTime).isLessThan(1); // Should average less than 1ms per request
        }
    }

    private MockHttpServletRequest createValidDPoPRequest() throws Exception {
        String accessToken = "valid.access.token";
        String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                testKeyPair, "GET", "http://localhost/api/v1/loans", accessToken);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/loans");
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer " + accessToken);
        request.addHeader("DPoP", dpopProof);

        return request;
    }

    private MockHttpServletRequest createInvalidDPoPRequest() throws Exception {
        String accessToken = "valid.access.token";
        String invalidDPoPProof = keyGenerator.createDPoPProofWithInvalidSignature(
                testKeyPair, "GET", "http://localhost/api/v1/loans");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/loans");
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer " + accessToken);
        request.addHeader("DPoP", invalidDPoPProof);

        return request;
    }
}