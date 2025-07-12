package com.amanahfi.platform.integration.regulatory;

import com.amanahfi.platform.integration.AbstractIntegrationTest;
import com.amanahfi.platform.regulatory.infrastructure.dto.vara.VaraApiResponse;
import com.amanahfi.platform.regulatory.infrastructure.dto.vara.VaraAssetRegistration;
import com.amanahfi.platform.regulatory.infrastructure.dto.vara.VaraComplianceSubmission;
import com.amanahfi.platform.regulatory.infrastructure.dto.vara.VaraHealthResponse;
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
 * Integration tests for VARA (Virtual Assets Regulatory Authority) API integration.
 * 
 * Tests the integration with UAE Virtual Assets Regulatory Authority APIs for:
 * - Virtual asset registration and compliance
 * - CBDC regulatory reporting
 * - Digital asset custody compliance
 * - Virtual asset service provider (VASP) requirements
 * - Cross-border digital asset transfers
 */
@Tag("integration")
@Tag("regulatory")
@Tag("vara")
@TestPropertySource(properties = {
    "regulatory.apis.vara.enabled=true",
    "regulatory.apis.vara.timeout=30000",
    "regulatory.apis.vara.retry.max-attempts=3"
})
class VaraApiIntegrationTest extends AbstractIntegrationTest {

    private static final String VARA_API_PATH = "/vara";

    @Test
    @DisplayName("Should successfully connect to VARA API health endpoint")
    void shouldConnectToVaraHealthEndpoint() {
        // Given
        String healthEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/health";
        
        // When
        ResponseEntity<VaraHealthResponse> response = restTemplate.getForEntity(
            healthEndpoint, 
            VaraHealthResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().status()).isEqualTo("OPERATIONAL"),
            () -> assertThat(response.getBody().apiVersion()).isNotNull(),
            () -> assertThat(response.getBody().timestamp()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should register virtual asset with VARA")
    void shouldRegisterVirtualAsset() throws JsonProcessingException {
        // Given
        VaraAssetRegistration registration = createVirtualAssetRegistration();
        String registerEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/assets/register";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-VARA-Entity-ID", "AMANAHFI-VASP-001");
        headers.set("X-VARA-License-Number", "VASP-LIC-2024-001");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<VaraAssetRegistration> request = new HttpEntity<>(registration, headers);
        
        // When
        ResponseEntity<VaraApiResponse> response = restTemplate.exchange(
            registerEndpoint,
            HttpMethod.POST,
            request,
            VaraApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().registrationId()).isNotNull(),
            () -> assertThat(response.getBody().status()).isEqualTo("PENDING_REVIEW")
        );
    }

    @Test
    @DisplayName("Should submit CBDC compliance report to VARA")
    void shouldSubmitCbdcComplianceReport() {
        // Given
        var cbdcReport = createCbdcComplianceReport();
        String submitEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/cbdc/compliance";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-VARA-Entity-ID", "AMANAHFI-VASP-001");
        headers.set("X-VARA-Report-Type", "CBDC_MONTHLY");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(cbdcReport, headers);
        
        // When
        ResponseEntity<VaraApiResponse> response = restTemplate.exchange(
            submitEndpoint,
            HttpMethod.POST,
            request,
            VaraApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().reportId()).isNotNull(),
            () -> assertThat(response.getBody().submissionDate()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should validate VASP compliance status with VARA")
    void shouldValidateVaspComplianceStatus() {
        // Given
        String vaspId = "AMANAHFI-VASP-001";
        String complianceEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/vasp/" + vaspId + "/compliance";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-VARA-Entity-ID", vaspId);
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            complianceEndpoint,
            HttpMethod.GET,
            request,
            Map.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsKey("complianceStatus"),
            () -> assertThat(response.getBody()).containsKey("licenseStatus"),
            () -> assertThat(response.getBody()).containsKey("lastReviewDate"),
            () -> assertThat(response.getBody().get("complianceStatus")).isIn("COMPLIANT", "PENDING", "NON_COMPLIANT")
        );
    }

    @Test
    @DisplayName("Should submit digital asset custody report to VARA")
    void shouldSubmitDigitalAssetCustodyReport() {
        // Given
        var custodyReport = createDigitalAssetCustodyReport();
        String custodyEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/custody/report";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-VARA-Entity-ID", "AMANAHFI-VASP-001");
        headers.set("X-VARA-Custody-License", "CUSTODY-LIC-2024-001");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(custodyReport, headers);
        
        // When
        ResponseEntity<VaraApiResponse> response = restTemplate.exchange(
            custodyEndpoint,
            HttpMethod.POST,
            request,
            VaraApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().custodyReportId()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should validate cross-border transfer compliance with VARA")
    void shouldValidateCrossBorderTransferCompliance() {
        // Given
        var transferDetails = createCrossBorderTransferDetails();
        String validateEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/transfers/validate";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-VARA-Entity-ID", "AMANAHFI-VASP-001");
        headers.set("X-VARA-Transfer-Type", "CROSS_BORDER");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(transferDetails, headers);
        
        // When
        ResponseEntity<VaraApiResponse> response = restTemplate.exchange(
            validateEndpoint,
            HttpMethod.POST,
            request,
            VaraApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().validationResult()).isIn("APPROVED", "REJECTED", "REQUIRES_REVIEW"),
            () -> assertThat(response.getBody().complianceScore()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should retrieve VARA regulatory updates")
    void shouldRetrieveRegulatoryUpdates() {
        // Given
        String updatesEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/regulatory/updates";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-VARA-Entity-ID", "AMANAHFI-VASP-001");
        headers.add("Accept", "application/json");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            updatesEndpoint,
            HttpMethod.GET,
            request,
            Map.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsKey("updates"),
            () -> assertThat(response.getBody()).containsKey("lastUpdated"),
            () -> assertThat(response.getBody()).containsKey("totalCount")
        );
    }

    @Test
    @DisplayName("Should handle VARA API authentication validation")
    void shouldValidateVaraAuthentication() {
        // Given
        String protectedEndpoint = getRegulatoryApiUrl() + VARA_API_PATH + "/vasp/profile";
        
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
        headers.set("X-VARA-Entity-ID", "AMANAHFI-VASP-001");
        
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

    private VaraAssetRegistration createVirtualAssetRegistration() {
        return new VaraAssetRegistration(
            UUID.randomUUID().toString(),
            "AMANAHFI-VASP-001",
            "DIGITAL_DIRHAM_REPRESENTATION",
            Map.of(
                "assetName", "AmanahFi Digital Dirham Token",
                "assetSymbol", "AFDD",
                "assetType", "CBDC_REPRESENTATION",
                "issuanceAuthority", "CBUAE",
                "maxSupply", new BigDecimal("1000000000.00"),
                "decimals", 18,
                "smartContractAddress", "0x742d35Cc6634C0532925a3b8D2A7d4A8B73e",
                "blockchainNetwork", "CORDA_CBDC_NETWORK",
                "complianceFeatures", new String[]{
                    "AML_COMPLIANT",
                    "KYC_INTEGRATED", 
                    "SANCTIONS_SCREENING",
                    "TRANSACTION_MONITORING"
                },
                "islamicFinanceCompliant", true,
                "shariaCompliantFeatures", new String[]{
                    "INTEREST_FREE",
                    "ASSET_BACKED",
                    "SHARIA_BOARD_APPROVED"
                }
            ),
            LocalDateTime.now()
        );
    }

    private Map<String, Object> createCbdcComplianceReport() {
        return Map.of(
            "reportId", UUID.randomUUID().toString(),
            "entityId", "AMANAHFI-VASP-001",
            "reportingPeriod", "2024-Q1",
            "cbdcActivity", Map.of(
                "totalWallets", 15420,
                "activeWallets", 12340,
                "totalTransactions", 98765,
                "totalVolume", Map.of(
                    "value", new BigDecimal("5420000.00"),
                    "currency", "AED-CBDC"
                ),
                "averageTransactionSize", Map.of(
                    "value", new BigDecimal("549.23"),
                    "currency", "AED-CBDC"
                )
            ),
            "complianceMetrics", Map.of(
                "kycCompletionRate", 0.98,
                "amlAlertsGenerated", 15,
                "suspiciousTransactionsReported", 3,
                "complianceTrainingCompleted", true,
                "auditFindings", 0
            ),
            "technicalMetrics", Map.of(
                "systemUptime", 0.9995,
                "transactionSuccessRate", 0.9998,
                "averageProcessingTime", "2.3s",
                "securityIncidents", 0
            ),
            "regulatoryCompliance", Map.of(
                "varaCompliance", "FULLY_COMPLIANT",
                "cbuaeCompliance", "FULLY_COMPLIANT",
                "fatfRecommendations", "IMPLEMENTED",
                "dataProtection", "GDPR_COMPLIANT"
            ),
            "submissionDate", LocalDateTime.now().toString(),
            "preparedBy", "Ahmed Al-Rashid, Chief Compliance Officer",
            "reviewedBy", "Fatima Al-Zahra, Head of Regulatory Affairs"
        );
    }

    private Map<String, Object> createDigitalAssetCustodyReport() {
        return Map.of(
            "custodyReportId", UUID.randomUUID().toString(),
            "entityId", "AMANAHFI-VASP-001",
            "reportingDate", LocalDateTime.now().toString(),
            "assetsUnderCustody", Map.of(
                "digitalDirham", Map.of(
                    "totalValue", new BigDecimal("25000000.00"),
                    "currency", "AED-CBDC",
                    "walletCount", 2340
                ),
                "islamicFinanceTokens", Map.of(
                    "murabaha", Map.of(
                        "totalValue", new BigDecimal("15000000.00"),
                        "currency", "AED",
                        "contractCount", 450
                    ),
                    "musharakah", Map.of(
                        "totalValue", new BigDecimal("8000000.00"),
                        "currency", "AED",
                        "contractCount", 120
                    )
                )
            ),
            "securityMeasures", Map.of(
                "coldStoragePercentage", 0.95,
                "multiSigWallets", true,
                "hsm", "LUNA_SA_7",
                "encryptionStandard", "AES-256",
                "keyManagement", "HIERARCHICAL_DETERMINISTIC",
                "backupProcedures", "GEOGRAPHICALLY_DISTRIBUTED"
            ),
            "custodyEvents", new Object[]{
                Map.of(
                    "eventType", "DEPOSIT",
                    "amount", new BigDecimal("500000.00"),
                    "currency", "AED-CBDC",
                    "timestamp", LocalDateTime.now().minusDays(1).toString()
                ),
                Map.of(
                    "eventType", "WITHDRAWAL",
                    "amount", new BigDecimal("250000.00"),
                    "currency", "AED-CBDC",
                    "timestamp", LocalDateTime.now().minusDays(2).toString()
                )
            },
            "complianceVerification", Map.of(
                "auditDate", LocalDateTime.now().minusDays(30).toString(),
                "auditor", "Ernst & Young UAE",
                "findings", "NO_MATERIAL_WEAKNESSES",
                "recommendations", new String[]{
                    "ENHANCE_MONITORING_DASHBOARD",
                    "QUARTERLY_PENETRATION_TESTING"
                }
            )
        );
    }

    private Map<String, Object> createCrossBorderTransferDetails() {
        return Map.of(
            "transferId", UUID.randomUUID().toString(),
            "transferType", "CROSS_BORDER_CBDC",
            "originatingEntity", "AMANAHFI-VASP-001",
            "receivingEntity", "FOREIGN-BANK-VASP-002",
            "amount", Map.of(
                "value", new BigDecimal("100000.00"),
                "currency", "AED-CBDC"
            ),
            "originCountry", "AE",
            "destinationCountry", "SA",
            "purpose", "ISLAMIC_FINANCE_SETTLEMENT",
            "sender", Map.of(
                "entityType", "CORPORATE",
                "name", "Al-Baraka Trading LLC",
                "nationalId", "784-2024-1234567",
                "address", Map.of(
                    "country", "AE",
                    "emirate", "DUBAI",
                    "area", "DIFC"
                )
            ),
            "receiver", Map.of(
                "entityType", "CORPORATE", 
                "name", "Riyadh Islamic Investments",
                "nationalId", "SAU-2024-7654321",
                "address", Map.of(
                    "country", "SA",
                    "city", "RIYADH",
                    "district", "FINANCIAL_DISTRICT"
                )
            ),
            "complianceChecks", Map.of(
                "sanctionsScreening", "CLEAR",
                "pepScreening", "CLEAR",
                "amlRiskScore", "LOW",
                "fatcaCompliance", "VERIFIED",
                "crsCompliance", "VERIFIED"
            ),
            "islamicFinanceCompliance", Map.of(
                "shariaCompliant", true,
                "underlyingAsset", "REAL_ESTATE_MUSHARAKAH",
                "profitSharingRatio", "60:40",
                "hsaApproval", "HSA-2024-IF-789"
            ),
            "regulatoryFramework", Map.of(
                "originatingRegulator", "VARA",
                "receivingRegulator", "SAMA",
                "bilateralAgreement", "UAE-SA-CBDC-FRAMEWORK-2024",
                "reportingRequirements", new String[]{
                    "ORIGIN_COUNTRY_REPORTING",
                    "DESTINATION_COUNTRY_REPORTING",
                    "FATF_TRAVEL_RULE"
                }
            )
        );
    }
}