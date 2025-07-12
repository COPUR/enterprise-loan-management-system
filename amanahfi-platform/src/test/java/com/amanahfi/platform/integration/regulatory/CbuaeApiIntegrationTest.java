package com.amanahfi.platform.integration.regulatory;

import com.amanahfi.platform.integration.AbstractIntegrationTest;
import com.amanahfi.platform.regulatory.infrastructure.dto.cbuae.CbuaeApiResponse;
import com.amanahfi.platform.regulatory.infrastructure.dto.cbuae.CbuaeComplianceSubmission;
import com.amanahfi.platform.regulatory.infrastructure.dto.cbuae.CbuaeHealthResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Integration tests for CBUAE (Central Bank of UAE) API integration.
 * 
 * Tests the integration with UAE Central Bank APIs for:
 * - Regulatory compliance reporting
 * - AML/KYC validation
 * - CBDC transaction reporting
 * - Suspicious Activity Reports (SAR)
 * - Customer Due Diligence (CDD)
 */
@Tag("integration")
@Tag("regulatory")
@Tag("cbuae")
@TestPropertySource(properties = {
    "regulatory.apis.cbuae.enabled=true",
    "regulatory.apis.cbuae.timeout=30000",
    "regulatory.apis.cbuae.retry.max-attempts=3"
})
class CbuaeApiIntegrationTest extends AbstractIntegrationTest {

    private static final String CBUAE_API_PATH = "/cbuae";

    @Test
    @DisplayName("Should successfully connect to CBUAE API health endpoint")
    void shouldConnectToCbuaeHealthEndpoint() {
        // Given
        String healthEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/health";
        
        // When
        ResponseEntity<CbuaeHealthResponse> response = restTemplate.getForEntity(
            healthEndpoint, 
            CbuaeHealthResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().status()).isEqualTo("OPERATIONAL"),
            () -> assertThat(response.getBody().timestamp()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should submit AML compliance report to CBUAE")
    void shouldSubmitAmlComplianceReport() throws JsonProcessingException {
        // Given
        CbuaeComplianceSubmission submission = createAmlComplianceSubmission();
        String submitEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/aml/submit";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBUAE-Institution-ID", "AMANAHFI-001");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<CbuaeComplianceSubmission> request = new HttpEntity<>(submission, headers);
        
        // When
        ResponseEntity<CbuaeApiResponse> response = restTemplate.exchange(
            submitEndpoint,
            HttpMethod.POST,
            request,
            CbuaeApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().referenceNumber()).isNotNull(),
            () -> assertThat(response.getBody().status()).isEqualTo("SUBMITTED")
        );
    }

    @Test
    @DisplayName("Should submit CBDC transaction report to CBUAE")
    void shouldSubmitCbdcTransactionReport() {
        // Given
        var cbdcReport = createCbdcTransactionReport();
        String submitEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/cbdc/transactions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBUAE-Institution-ID", "AMANAHFI-001");
        headers.set("X-CBDC-Network", "DIGITAL-DIRHAM");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(cbdcReport, headers);
        
        // When
        ResponseEntity<CbuaeApiResponse> response = restTemplate.exchange(
            submitEndpoint,
            HttpMethod.POST,
            request,
            CbuaeApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().transactionId()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should submit Suspicious Activity Report (SAR) to CBUAE")
    void shouldSubmitSuspiciousActivityReport() {
        // Given
        var sarReport = createSuspiciousActivityReport();
        String sarEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/sar/submit";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBUAE-Institution-ID", "AMANAHFI-001");
        headers.set("X-SAR-Priority", "HIGH");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(sarReport, headers);
        
        // When
        ResponseEntity<CbuaeApiResponse> response = restTemplate.exchange(
            sarEndpoint,
            HttpMethod.POST,
            request,
            CbuaeApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().sarReferenceNumber()).isNotNull(),
            () -> assertThat(response.getBody().priority()).isEqualTo("HIGH")
        );
    }

    @Test
    @DisplayName("Should validate customer due diligence with CBUAE")
    void shouldValidateCustomerDueDiligence() {
        // Given
        var cddRequest = createCustomerDueDiligenceRequest();
        String cddEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/cdd/validate";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBUAE-Institution-ID", "AMANAHFI-001");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(cddRequest, headers);
        
        // When
        ResponseEntity<CbuaeApiResponse> response = restTemplate.exchange(
            cddEndpoint,
            HttpMethod.POST,
            request,
            CbuaeApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().cddStatus()).isIn("APPROVED", "PENDING", "REQUIRES_ENHANCEMENT")
        );
    }

    @Test
    @DisplayName("Should handle CBUAE API rate limiting gracefully")
    void shouldHandleRateLimiting() {
        // Given
        String healthEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/health";
        
        // When - Make multiple rapid requests to trigger rate limiting
        for (int i = 0; i < 10; i++) {
            ResponseEntity<CbuaeHealthResponse> response = restTemplate.getForEntity(
                healthEndpoint, 
                CbuaeHealthResponse.class
            );
            
            // Then - Should either succeed or return rate limit error
            assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Test
    @DisplayName("Should validate CBUAE API authentication")
    void shouldValidateAuthentication() {
        // Given
        String protectedEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/institutions/profile";
        
        // When - Call without authentication
        ResponseEntity<String> responseWithoutAuth = restTemplate.getForEntity(
            protectedEndpoint, 
            String.class
        );
        
        // Then - Should return unauthorized
        assertThat(responseWithoutAuth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // When - Call with authentication
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBUAE-Institution-ID", "AMANAHFI-001");
        
        HttpEntity<Void> requestWithAuth = new HttpEntity<>(headers);
        ResponseEntity<String> responseWithAuth = restTemplate.exchange(
            protectedEndpoint,
            HttpMethod.GET,
            requestWithAuth,
            String.class
        );
        
        // Then - Should succeed
        assertThat(responseWithAuth.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should retrieve regulatory limits from CBUAE")
    void shouldRetrieveRegulatoryLimits() {
        // Given
        String limitsEndpoint = getRegulatoryApiUrl() + CBUAE_API_PATH + "/limits/transaction";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBUAE-Institution-ID", "AMANAHFI-001");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            limitsEndpoint,
            HttpMethod.GET,
            request,
            Map.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsKey("dailyLimit"),
            () -> assertThat(response.getBody()).containsKey("monthlyLimit"),
            () -> assertThat(response.getBody()).containsKey("sarThreshold")
        );
    }

    private CbuaeComplianceSubmission createAmlComplianceSubmission() {
        return new CbuaeComplianceSubmission(
            UUID.randomUUID().toString(),
            "AMANAHFI-001",
            "AML_COMPLIANCE",
            Map.of(
                "reportingPeriod", "2024-Q1",
                "totalTransactions", 15420,
                "suspiciousTransactions", 3,
                "sarsFiled", 2,
                "complianceOfficer", "Jane Doe",
                "reviewDate", LocalDateTime.now().toString()
            ),
            LocalDateTime.now()
        );
    }

    private Map<String, Object> createCbdcTransactionReport() {
        return Map.of(
            "transactionId", UUID.randomUUID().toString(),
            "networkType", "DIGITAL_DIRHAM",
            "transactionType", "TRANSFER",
            "amount", Map.of(
                "value", new BigDecimal("1000.00"),
                "currency", "AED-CBDC"
            ),
            "sender", Map.of(
                "walletId", UUID.randomUUID().toString(),
                "institutionId", "AMANAHFI-001"
            ),
            "receiver", Map.of(
                "walletId", UUID.randomUUID().toString(),
                "institutionId", "OTHER-BANK-001"
            ),
            "timestamp", LocalDateTime.now().toString(),
            "blockchainHash", "0x1234567890abcdef",
            "cordaTransactionId", UUID.randomUUID().toString()
        );
    }

    private Map<String, Object> createSuspiciousActivityReport() {
        return Map.of(
            "sarId", UUID.randomUUID().toString(),
            "customerId", "CUST-" + UUID.randomUUID().toString(),
            "suspiciousActivity", "UNUSUAL_TRANSACTION_PATTERN",
            "description", "Multiple large cash deposits followed by immediate wire transfers",
            "transactionIds", new String[]{
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
            },
            "totalAmount", Map.of(
                "value", new BigDecimal("250000.00"),
                "currency", "AED"
            ),
            "reportingOfficer", "John Smith",
            "dateIdentified", LocalDateTime.now().minusDays(1).toString(),
            "priority", "HIGH",
            "riskFactors", new String[]{
                "HIGH_VALUE_TRANSACTIONS",
                "RAPID_MOVEMENT_OF_FUNDS",
                "INCONSISTENT_WITH_CUSTOMER_PROFILE"
            }
        );
    }

    private Map<String, Object> createCustomerDueDiligenceRequest() {
        return Map.of(
            "customerId", "CUST-" + UUID.randomUUID().toString(),
            "nationalId", "784-1234-5678901-2",
            "fullName", "Ahmed Al Rashid",
            "dateOfBirth", "1985-03-15",
            "nationality", "AE",
            "residencyStatus", "RESIDENT",
            "occupation", "BUSINESS_OWNER",
            "expectedTransactionVolume", Map.of(
                "monthly", new BigDecimal("50000.00"),
                "currency", "AED"
            ),
            "sourceOfFunds", "BUSINESS_INCOME",
            "riskProfile", "MEDIUM",
            "pepStatus", false,
            "sanctionsScreening", "CLEAR"
        );
    }
}