package com.amanahfi.platform.integration.regulatory;

import com.amanahfi.platform.integration.AbstractIntegrationTest;
import com.amanahfi.platform.regulatory.infrastructure.dto.hsa.HsaApiResponse;
import com.amanahfi.platform.regulatory.infrastructure.dto.hsa.HsaComplianceSubmission;
import com.amanahfi.platform.regulatory.infrastructure.dto.hsa.HsaHealthResponse;
import com.amanahfi.platform.regulatory.infrastructure.dto.hsa.HsaShariaValidation;
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
 * Integration tests for HSA (Higher Sharia Authority) API integration.
 * 
 * Tests the integration with Higher Sharia Authority APIs for:
 * - Sharia compliance validation
 * - Islamic Finance product certification
 * - Fatwa verification and updates
 * - Halal asset screening
 * - Profit distribution validation
 * - Islamic banking standards compliance
 */
@Tag("integration")
@Tag("regulatory")
@Tag("hsa")
@Tag("islamic-finance")
@TestPropertySource(properties = {
    "regulatory.apis.hsa.enabled=true",
    "regulatory.apis.hsa.timeout=30000",
    "regulatory.apis.hsa.retry.max-attempts=3",
    "islamic-finance.strict-mode=true"
})
class HsaApiIntegrationTest extends AbstractIntegrationTest {

    private static final String HSA_API_PATH = "/hsa";

    @Test
    @DisplayName("Should successfully connect to HSA API health endpoint")
    void shouldConnectToHsaHealthEndpoint() {
        // Given
        String healthEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/health";
        
        // When
        ResponseEntity<HsaHealthResponse> response = restTemplate.getForEntity(
            healthEndpoint, 
            HsaHealthResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().status()).isEqualTo("OPERATIONAL"),
            () -> assertThat(response.getBody().shariaBoard()).isEqualTo("ACTIVE"),
            () -> assertThat(response.getBody().fatwaDatabaseVersion()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should validate Murabaha contract for Sharia compliance")
    void shouldValidateMurabahaContract() throws JsonProcessingException {
        // Given
        HsaShariaValidation validation = createMurabahaValidationRequest();
        String validateEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/validate/murabaha";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        headers.set("X-HSA-Scholar-ID", "SCHOLAR-001");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<HsaShariaValidation> request = new HttpEntity<>(validation, headers);
        
        // When
        ResponseEntity<HsaApiResponse> response = restTemplate.exchange(
            validateEndpoint,
            HttpMethod.POST,
            request,
            HsaApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().complianceStatus()).isEqualTo("SHARIA_COMPLIANT"),
            () -> assertThat(response.getBody().validationId()).isNotNull(),
            () -> assertThat(response.getBody().fatwReference()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should validate Musharakah partnership for Sharia compliance")
    void shouldValidateMusharakahPartnership() {
        // Given
        var musharakahContract = createMusharakahContract();
        String validateEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/validate/musharakah";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        headers.set("X-HSA-Contract-Type", "MUSHARAKAH");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(musharakahContract, headers);
        
        // When
        ResponseEntity<HsaApiResponse> response = restTemplate.exchange(
            validateEndpoint,
            HttpMethod.POST,
            request,
            HsaApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().complianceStatus()).isIn("SHARIA_COMPLIANT", "REQUIRES_MODIFICATION"),
            () -> assertThat(response.getBody().profitSharingValidation()).isTrue()
        );
    }

    @Test
    @DisplayName("Should screen asset for Halal compliance")
    void shouldScreenAssetForHalalCompliance() {
        // Given
        var assetDetails = createAssetScreeningRequest();
        String screenEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/screen/asset";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        headers.set("X-HSA-Screening-Level", "COMPREHENSIVE");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(assetDetails, headers);
        
        // When
        ResponseEntity<HsaApiResponse> response = restTemplate.exchange(
            screenEndpoint,
            HttpMethod.POST,
            request,
            HsaApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().halalStatus()).isIn("HALAL", "HARAM", "DOUBTFUL"),
            () -> assertThat(response.getBody().screeningScore()).isBetween(0.0, 100.0),
            () -> assertThat(response.getBody().prohibitedElements()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should submit Islamic Finance product for certification")
    void shouldSubmitProductForCertification() {
        // Given
        var productDetails = createIslamicFinanceProduct();
        String certifyEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/certify/product";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        headers.set("X-HSA-Product-Category", "FINANCING");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(productDetails, headers);
        
        // When
        ResponseEntity<HsaApiResponse> response = restTemplate.exchange(
            certifyEndpoint,
            HttpMethod.POST,
            request,
            HsaApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().certificationId()).isNotNull(),
            () -> assertThat(response.getBody().estimatedCompletionDate()).isNotNull(),
            () -> assertThat(response.getBody().status()).isEqualTo("UNDER_REVIEW")
        );
    }

    @Test
    @DisplayName("Should validate profit distribution calculation")
    void shouldValidateProfitDistribution() {
        // Given
        var profitDistribution = createProfitDistributionCalculation();
        String validateEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/validate/profit-distribution";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        headers.set("X-HSA-Calculation-Method", "MUDARABAH");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(profitDistribution, headers);
        
        // When
        ResponseEntity<HsaApiResponse> response = restTemplate.exchange(
            validateEndpoint,
            HttpMethod.POST,
            request,
            HsaApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().calculationValid()).isTrue(),
            () -> assertThat(response.getBody().shariaCompliantRatio()).isTrue(),
            () -> assertThat(response.getBody().recommendedAdjustments()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should retrieve latest Fatwa updates")
    void shouldRetrieveLatestFatwaUpdates() {
        // Given
        String fatwaEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/fatwa/updates";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        headers.set("X-HSA-Category", "BANKING_FINANCE");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            fatwaEndpoint,
            HttpMethod.GET,
            request,
            Map.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsKey("fatwas"),
            () -> assertThat(response.getBody()).containsKey("lastUpdated"),
            () -> assertThat(response.getBody()).containsKey("totalCount"),
            () -> assertThat(response.getBody()).containsKey("categories")
        );
    }

    @Test
    @DisplayName("Should validate Ijarah lease contract")
    void shouldValidateIjarahLeaseContract() {
        // Given
        var ijarahContract = createIjarahContract();
        String validateEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/validate/ijarah";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        headers.set("X-HSA-Lease-Type", "IJARAH_MUNTAHIA_BITTAMLEEK");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(ijarahContract, headers);
        
        // When
        ResponseEntity<HsaApiResponse> response = restTemplate.exchange(
            validateEndpoint,
            HttpMethod.POST,
            request,
            HsaApiResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().complianceStatus()).isEqualTo("SHARIA_COMPLIANT"),
            () -> assertThat(response.getBody().ownershipTransferValid()).isTrue(),
            () -> assertThat(response.getBody().rentalCalculationValid()).isTrue()
        );
    }

    @Test
    @DisplayName("Should handle HSA API authentication")
    void shouldHandleHsaAuthentication() {
        // Given
        String protectedEndpoint = getRegulatoryApiUrl() + HSA_API_PATH + "/institution/profile";
        
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
        headers.set("X-HSA-Institution-ID", "AMANAHFI-001");
        
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

    private HsaShariaValidation createMurabahaValidationRequest() {
        return new HsaShariaValidation(
            UUID.randomUUID().toString(),
            "AMANAHFI-001",
            "MURABAHA",
            Map.of(
                "asset", Map.of(
                    "type", "REAL_ESTATE",
                    "description", "Residential property in Dubai Marina",
                    "value", new BigDecimal("2500000.00"),
                    "currency", "AED",
                    "halalVerified", true
                ),
                "pricing", Map.of(
                    "costPrice", new BigDecimal("2500000.00"),
                    "profitMargin", new BigDecimal("250000.00"),
                    "profitRate", 0.10,
                    "sellingPrice", new BigDecimal("2750000.00"),
                    "transparentPricing", true
                ),
                "terms", Map.of(
                    "paymentPeriod", "60 months",
                    "installmentAmount", new BigDecimal("45833.33"),
                    "earlyPaymentAllowed", true,
                    "latePaymentPenalty", "NONE",
                    "ownershipTransferPoint", "IMMEDIATE"
                ),
                "documentation", Map.of(
                    "propertyDeed", "VERIFIED",
                    "valuationReport", "INDEPENDENT_VALUER",
                    "insurancePolicy", "TAKAFUL_COVERED",
                    "registrationFees", "INCLUDED"
                )
            ),
            LocalDateTime.now()
        );
    }

    private Map<String, Object> createMusharakahContract() {
        return Map.of(
            "contractId", UUID.randomUUID().toString(),
            "contractType", "MUSHARAKAH_MUTANAQISAH",
            "partners", new Object[]{
                Map.of(
                    "name", "AmanahFi Platform",
                    "type", "FINANCIAL_INSTITUTION",
                    "capitalContribution", new BigDecimal("1800000.00"),
                    "ownershipPercentage", 0.72,
                    "managementRole", "SILENT_PARTNER"
                ),
                Map.of(
                    "name", "Ahmed Al-Rashid",
                    "type", "INDIVIDUAL",
                    "capitalContribution", new BigDecimal("700000.00"),
                    "ownershipPercentage", 0.28,
                    "managementRole", "MANAGING_PARTNER"
                )
            ),
            "project", Map.of(
                "description", "Islamic Finance Technology Development",
                "sector", "FINTECH",
                "location", "Dubai International Financial Centre",
                "expectedDuration", "5 years",
                "businessPlan", "SHARIA_BOARD_APPROVED"
            ),
            "profitSharing", Map.of(
                "profitRatio", "60:40",
                "lossSharing", "PROPORTIONAL_TO_CAPITAL",
                "distributionFrequency", "QUARTERLY",
                "reinvestmentPolicy", "MUTUAL_CONSENT"
            ),
            "governance", Map.of(
                "managementStructure", "BOARD_OF_DIRECTORS",
                "decisionMaking", "MAJORITY_VOTE",
                "auditRequirements", "ANNUAL_SHARIA_AUDIT",
                "disputeResolution", "ISLAMIC_ARBITRATION"
            ),
            "exitStrategy", Map.of(
                "buyoutOption", "AVAILABLE",
                "valuationMethod", "INDEPENDENT_VALUATION",
                "firstRightOfRefusal", "PARTNERS",
                "gradualExit", "ALLOWED"
            )
        );
    }

    private Map<String, Object> createAssetScreeningRequest() {
        return Map.of(
            "assetId", UUID.randomUUID().toString(),
            "assetType", "CORPORATE_EQUITY",
            "company", Map.of(
                "name", "Emirates Renewable Energy Corp",
                "ticker", "EREC",
                "sector", "RENEWABLE_ENERGY",
                "market", "ADX",
                "country", "AE"
            ),
            "businessActivities", new String[]{
                "SOLAR_POWER_GENERATION",
                "WIND_ENERGY_DEVELOPMENT",
                "ENERGY_STORAGE_SOLUTIONS",
                "GREEN_HYDROGEN_PRODUCTION"
            },
            "financialMetrics", Map.of(
                "totalDebt", new BigDecimal("500000000.00"),
                "interestBasedDebt", new BigDecimal("0.00"),
                "shariaCompliantFinancing", new BigDecimal("500000000.00"),
                "debtToEquityRatio", 0.30,
                "interestIncomePercentage", 0.00
            ),
            "revenueStreams", Map.of(
                "operatingRevenue", new BigDecimal("2000000000.00"),
                "halalRevenue", new BigDecimal("2000000000.00"),
                "haramRevenue", new BigDecimal("0.00"),
                "doubtfulRevenue", new BigDecimal("0.00"),
                "halalPercentage", 1.00
            ),
            "complianceChecks", Map.of(
                "alcoholProduction", false,
                "tobaccoInvolvement", false,
                "gamblingActivities", false,
                "adultEntertainment", false,
                "conventionalBanking", false,
                "weaponsManufacturing", false,
                "porkProcessing", false
            ),
            "sustainabilityMetrics", Map.of(
                "esgScore", 85.5,
                "carbonNeutral", true,
                "socialImpact", "POSITIVE",
                "environmentalImpact", "HIGHLY_POSITIVE"
            )
        );
    }

    private Map<String, Object> createIslamicFinanceProduct() {
        return Map.of(
            "productId", UUID.randomUUID().toString(),
            "productName", "AmanahFi Digital Murabaha Home Financing",
            "productCategory", "HOME_FINANCING",
            "structure", "MURABAHA",
            "features", Map.of(
                "digitalOnboarding", true,
                "instantPreApproval", true,
                "flexiblePayment", true,
                "earlySettlement", true,
                "takafulIntegration", true,
                "cbdcPayments", true
            ),
            "shariaCompliance", Map.of(
                "assetBacked", true,
                "interestFree", true,
                "ghararFree", true,
                "transparentPricing", true,
                "ownershipTransfer", "IMMEDIATE",
                "riskSharing", "COMPLIANT"
            ),
            "eligibilityCriteria", Map.of(
                "minAge", 21,
                "maxAge", 65,
                "minIncome", new BigDecimal("15000.00"),
                "maxFinancing", new BigDecimal("10000000.00"),
                "citizenship", new String[]{"AE", "GCC"},
                "employmentType", new String[]{"EMPLOYED", "SELF_EMPLOYED", "BUSINESS_OWNER"}
            ),
            "pricingStructure", Map.of(
                "profitRateRange", "3.5% - 8.5%",
                "processingFee", new BigDecimal("2500.00"),
                "valuationFee", new BigDecimal("1500.00"),
                "administrativeFee", "NONE",
                "latePaymentPenalty", "CHARITY_DONATION",
                "earlySettlementDiscount", "CUSTOMER_BENEFIT"
            ),
            "technologyFeatures", Map.of(
                "mobileApp", "FULL_FEATURED",
                "webPortal", "COMPREHENSIVE",
                "aiPowered", true,
                "blockchainSettlement", true,
                "cbdcIntegration", true,
                "digitalSignature", true
            ),
            "regulatoryCompliance", Map.of(
                "cbuaeApproved", true,
                "hsaCertified", false,
                "varaCompliant", true,
                "aaoifiStandards", true,
                "islamiDbCompliant", true
            )
        );
    }

    private Map<String, Object> createProfitDistributionCalculation() {
        return Map.of(
            "calculationId", UUID.randomUUID().toString(),
            "contractType", "MUDARABAH",
            "investmentDetails", Map.of(
                "totalInvestment", new BigDecimal("10000000.00"),
                "investmentPeriod", "12 months",
                "businessSector", "HALAL_FOOD_MANUFACTURING",
                "expectedReturn", new BigDecimal("1200000.00")
            ),
            "partnerContributions", Map.of(
                "rabbulMal", Map.of(
                    "capital", new BigDecimal("10000000.00"),
                    "percentage", 100.0,
                    "role", "CAPITAL_PROVIDER"
                ),
                "mudarib", Map.of(
                    "capital", new BigDecimal("0.00"),
                    "expertise", "BUSINESS_MANAGEMENT",
                    "role", "FUND_MANAGER"
                )
            ),
            "profitSharingRatio", Map.of(
                "rabbulMal", 0.70,
                "mudarib", 0.30,
                "agreementDate", LocalDateTime.now().minusMonths(1).toString(),
                "ratioJustification", "INDUSTRY_STANDARD_MANAGEMENT_FEE"
            ),
            "actualResults", Map.of(
                "totalProfit", new BigDecimal("1500000.00"),
                "operatingExpenses", new BigDecimal("300000.00"),
                "netProfit", new BigDecimal("1200000.00"),
                "distributionDate", LocalDateTime.now().toString()
            ),
            "proposedDistribution", Map.of(
                "rabbulMalShare", new BigDecimal("840000.00"),
                "mudaribShare", new BigDecimal("360000.00"),
                "calculationMethod", "NET_PROFIT_BASED",
                "taxConsiderations", "ZAKAT_APPLICABLE"
            ),
            "complianceVerification", Map.of(
                "transparencyMaintained", true,
                "consensualAgreement", true,
                "islamicAccountingStandards", "AAOIFI_COMPLIANT",
                "auditTrail", "COMPLETE"
            )
        );
    }

    private Map<String, Object> createIjarahContract() {
        return Map.of(
            "contractId", UUID.randomUUID().toString(),
            "contractType", "IJARAH_MUNTAHIA_BITTAMLEEK",
            "asset", Map.of(
                "type", "COMMERCIAL_VEHICLE",
                "description", "Mercedes-Benz Actros Truck",
                "value", new BigDecimal("450000.00"),
                "currency", "AED",
                "condition", "NEW",
                "specifications", "Euro 6 Emission Standard, Halal Transport Certified"
            ),
            "lessor", Map.of(
                "name", "AmanahFi Equipment Leasing",
                "role", "ASSET_OWNER",
                "responsibilities", new String[]{
                    "ASSET_MAINTENANCE",
                    "INSURANCE_COVERAGE",
                    "MAJOR_REPAIRS"
                }
            ),
            "lessee", Map.of(
                "name", "Dubai Halal Logistics LLC",
                "businessType", "HALAL_FOOD_DISTRIBUTION",
                "role", "ASSET_USER",
                "responsibilities", new String[]{
                    "ROUTINE_MAINTENANCE",
                    "SAFE_OPERATION",
                    "TIMELY_PAYMENTS"
                }
            ),
            "leaseTerms", Map.of(
                "leasePeriod", "60 months",
                "monthlyRental", new BigDecimal("9500.00"),
                "totalRentals", new BigDecimal("570000.00"),
                "securityDeposit", new BigDecimal("28500.00"),
                "renewalOption", "AUTOMATIC"
            ),
            "ownershipTransfer", Map.of(
                "transferOption", "MANDATORY",
                "transferPrice", new BigDecimal("1.00"),
                "transferConditions", new String[]{
                    "ALL_RENTALS_PAID",
                    "ASSET_CONDITION_ACCEPTABLE",
                    "NO_BREACH_OF_CONTRACT"
                },
                "transferDocumentation", "INCLUDED_IN_CONTRACT"
            ),
            "riskAllocation", Map.of(
                "operationalRisk", "LESSEE",
                "assetRisk", "LESSOR",
                "marketRisk", "SHARED",
                "insuranceRequirement", "COMPREHENSIVE_TAKAFUL"
            ),
            "complianceFeatures", Map.of(
                "assetOwnership", "CLEARLY_ESTABLISHED",
                "rentalCalculation", "ASSET_BASED",
                "penaltyStructure", "CHARITY_BASED",
                "disputeResolution", "ISLAMIC_ARBITRATION"
            )
        );
    }
}