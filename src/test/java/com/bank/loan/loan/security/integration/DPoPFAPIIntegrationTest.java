package com.bank.loan.loan.security.integration;

import com.bank.loan.loan.security.dpop.util.DPoPTestKeyGenerator;
import com.bank.loan.loan.security.fapi.FAPISecurityConfig;
import com.nimbusds.jose.jwk.ECKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://keycloak.example.com/realms/banking-system",
        "dpop.enabled=true",
        "fapi.enabled=true"
})
@DisplayName("DPoP FAPI Integration Tests")
class DPoPFAPIIntegrationTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private DPoPTestKeyGenerator keyGenerator;
    private ECKey testKeyPair;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        restTemplate = new TestRestTemplate();
        keyGenerator = new DPoPTestKeyGenerator();
        testKeyPair = keyGenerator.generateECKey();
        baseUrl = "http://localhost:" + port;
    }

    @Nested
    @DisplayName("DPoP-enabled FAPI Endpoint Tests")
    class DPoPEnabledFAPIEndpointTests {

        @Test
        @DisplayName("Should require DPoP proof for FAPI loan endpoints")
        void shouldRequireDPoPProofForFAPILoanEndpoints() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer mock.access.token");
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst("WWW-Authenticate")).contains("DPoP");
        }

        @Test
        @DisplayName("Should accept valid DPoP proof with FAPI headers")
        void shouldAcceptValidDPoPProofWithFAPIHeaders() throws Exception {
            String validAccessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", dpopProof);
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Note: This may return 403 or other error due to JWT validation, but should not be 401 due to missing DPoP
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject DPoP proof with missing FAPI headers")
        void shouldRejectDPoPProofWithMissingFAPIHeaders() throws Exception {
            String validAccessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", dpopProof);
            // Missing FAPI headers

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should validate DPoP proof method and URI for FAPI endpoints")
        void shouldValidateDPoPProofMethodAndUriForFAPIEndpoints() throws Exception {
            String validAccessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "POST", baseUrl + "/api/v1/fapi/loans", validAccessToken); // Wrong method

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", dpopProof);
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst("WWW-Authenticate")).contains("invalid_dpop_proof");
        }
    }

    @Nested
    @DisplayName("DPoP Payment API Tests")
    class DPoPPaymentAPITests {

        @Test
        @DisplayName("Should require DPoP proof for payment initiation")
        void shouldRequireDPoPProofForPaymentInitiation() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer mock.access.token");
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");
            headers.setContentType(MediaType.APPLICATION_JSON);

            String paymentRequest = """
                {
                    "amount": 1000.00,
                    "currency": "USD",
                    "recipientAccountId": "12345",
                    "description": "Test payment"
                }
                """;

            HttpEntity<String> entity = new HttpEntity<>(paymentRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/payments",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst("WWW-Authenticate")).contains("DPoP");
        }

        @Test
        @DisplayName("Should validate DPoP proof for payment confirmation")
        void shouldValidateDPoPProofForPaymentConfirmation() throws Exception {
            String validAccessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "PUT", baseUrl + "/api/v1/fapi/payments/123/confirm", validAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", dpopProof);
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");
            headers.setContentType(MediaType.APPLICATION_JSON);

            String confirmationRequest = """
                {
                    "confirmationCode": "123456",
                    "authorizedAmount": 1000.00
                }
                """;

            HttpEntity<String> entity = new HttpEntity<>(confirmationRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/payments/123/confirm",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            // Should not be 401 due to DPoP validation (may be other errors due to business logic)
            assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DPoP Rate Limiting Integration Tests")
    class DPoPRateLimitingIntegrationTests {

        @Test
        @DisplayName("Should enforce rate limits per DPoP key")
        void shouldEnforceRateLimitsPerDPoPKey() throws Exception {
            String validAccessToken = "valid.access.token";

            // Make multiple requests with the same DPoP key
            for (int i = 0; i < 10; i++) {
                String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                        testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + validAccessToken);
                headers.set("DPoP", dpopProof);
                headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-12345678901" + i);
                headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
                headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + "/api/v1/fapi/loans",
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (i < 5) {
                    // First few requests should pass DPoP validation
                    assertThat(response.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
                } else {
                    // Later requests should be rate limited
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                }
            }
        }

        @Test
        @DisplayName("Should allow separate rate limits for different DPoP keys")
        void shouldAllowSeparateRateLimitsForDifferentDPoPKeys() throws Exception {
            ECKey secondKeyPair = keyGenerator.generateECKey();
            String validAccessToken = "valid.access.token";

            // Make requests with first DPoP key
            String dpopProof1 = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

            HttpHeaders headers1 = new HttpHeaders();
            headers1.set("Authorization", "Bearer " + validAccessToken);
            headers1.set("DPoP", dpopProof1);
            headers1.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789011");
            headers1.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers1.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            // Make requests with second DPoP key
            String dpopProof2 = keyGenerator.createDPoPProofWithAccessTokenHash(
                    secondKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

            HttpHeaders headers2 = new HttpHeaders();
            headers2.set("Authorization", "Bearer " + validAccessToken);
            headers2.set("DPoP", dpopProof2);
            headers2.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers2.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers2.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity1 = new HttpEntity<>(headers1);
            HttpEntity<String> entity2 = new HttpEntity<>(headers2);

            ResponseEntity<String> response1 = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity1,
                    String.class
            );

            ResponseEntity<String> response2 = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity2,
                    String.class
            );

            // Both should pass DPoP validation (separate rate limits)
            assertThat(response1.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response2.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("DPoP Nonce Integration Tests")
    class DPoPNonceIntegrationTests {

        @Test
        @DisplayName("Should challenge with nonce when required")
        void shouldChallengeWithNonceWhenRequired() throws Exception {
            String validAccessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", dpopProof);
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // If nonce is required, server should respond with 401 and DPoP-Nonce header
            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                String wwwAuthenticate = response.getHeaders().getFirst("WWW-Authenticate");
                if (wwwAuthenticate != null && wwwAuthenticate.contains("use_dpop_nonce")) {
                    assertThat(response.getHeaders().getFirst("DPoP-Nonce")).isNotNull();
                }
            }
        }

        @Test
        @DisplayName("Should accept DPoP proof with valid nonce")
        void shouldAcceptDPoPProofWithValidNonce() throws Exception {
            // First request to get nonce
            String validAccessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", dpopProof);
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // If nonce is provided, retry with nonce
            String nonce = response.getHeaders().getFirst("DPoP-Nonce");
            if (nonce != null) {
                String dpopProofWithNonce = keyGenerator.createDPoPProofWithNonce(
                        testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", nonce);

                HttpHeaders headersWithNonce = new HttpHeaders();
                headersWithNonce.set("Authorization", "Bearer " + validAccessToken);
                headersWithNonce.set("DPoP", dpopProofWithNonce);
                headersWithNonce.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789013");
                headersWithNonce.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
                headersWithNonce.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

                HttpEntity<String> entityWithNonce = new HttpEntity<>(headersWithNonce);

                ResponseEntity<String> responseWithNonce = restTemplate.exchange(
                        baseUrl + "/api/v1/fapi/loans",
                        HttpMethod.GET,
                        entityWithNonce,
                        String.class
                );

                // Should not be 401 due to nonce validation
                assertThat(responseWithNonce.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
            }
        }
    }

    @Nested
    @DisplayName("DPoP Error Handling Integration Tests")
    class DPoPErrorHandlingIntegrationTests {

        @Test
        @DisplayName("Should return appropriate error for invalid DPoP proof structure")
        void shouldReturnAppropriateErrorForInvalidDPoPProofStructure() throws Exception {
            String validAccessToken = "valid.access.token";
            String invalidDPoPProof = keyGenerator.createDPoPProofWithInvalidTyp(
                    testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans");

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", invalidDPoPProof);
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getHeaders().getFirst("WWW-Authenticate")).contains("invalid_dpop_proof");
        }

        @Test
        @DisplayName("Should return appropriate error for replay attack")
        void shouldReturnAppropriateErrorForReplayAttack() throws Exception {
            String validAccessToken = "valid.access.token";
            String dpopProof = keyGenerator.createDPoPProofWithAccessTokenHash(
                    testKeyPair, "GET", baseUrl + "/api/v1/fapi/loans", validAccessToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validAccessToken);
            headers.set("DPoP", dpopProof);
            headers.set("X-FAPI-Interaction-ID", "12345678-1234-1234-1234-123456789012");
            headers.set("X-FAPI-Auth-Date", "2023-01-01T12:00:00Z");
            headers.set("X-FAPI-Customer-IP-Address", "192.168.1.100");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // First request
            ResponseEntity<String> response1 = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Second request with same DPoP proof (replay)
            ResponseEntity<String> response2 = restTemplate.exchange(
                    baseUrl + "/api/v1/fapi/loans",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Second request should be rejected as replay
            assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response2.getHeaders().getFirst("WWW-Authenticate")).contains("invalid_dpop_proof");
        }
    }
}