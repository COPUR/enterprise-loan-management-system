package com.amanahfi.platform.integration.cbdc;

import com.amanahfi.platform.integration.AbstractIntegrationTest;
import com.amanahfi.platform.cbdc.infrastructure.dto.corda.CordaNetworkStatus;
import com.amanahfi.platform.cbdc.infrastructure.dto.corda.CordaTransactionResponse;
import com.amanahfi.platform.cbdc.infrastructure.dto.corda.DigitalDirhamTransfer;
import com.amanahfi.platform.cbdc.infrastructure.dto.corda.NetworkNodeInfo;
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
 * Integration tests for Corda Network integration for CBDC operations.
 * 
 * Tests the integration with Corda Distributed Ledger for:
 * - Digital Dirham wallet management
 * - CBDC transfers and settlements
 * - Cross-border payments
 * - Smart contract execution
 * - Network consensus and validation
 * - Regulatory reporting and compliance
 */
@Tag("integration")
@Tag("cbdc")
@Tag("corda")
@Tag("blockchain")
@TestPropertySource(properties = {
    "cbdc.corda.network.enabled=true",
    "cbdc.corda.network.timeout=45000",
    "cbdc.corda.network.retry.max-attempts=3",
    "cbdc.digital-dirham.test-mode=true"
})
class CordaNetworkIntegrationTest extends AbstractIntegrationTest {

    private static final String CORDA_API_PATH = "/corda";

    @Test
    @DisplayName("Should successfully connect to Corda network")
    void shouldConnectToCordaNetwork() {
        // Given
        String networkStatusEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/network/status";
        
        // When
        ResponseEntity<CordaNetworkStatus> response = restTemplate.getForEntity(
            networkStatusEndpoint, 
            CordaNetworkStatus.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().networkStatus()).isEqualTo("OPERATIONAL"),
            () -> assertThat(response.getBody().nodeCount()).isGreaterThan(0),
            () -> assertThat(response.getBody().consensusAlgorithm()).isEqualTo("NOTARY_CONSENSUS"),
            () -> assertThat(response.getBody().lastBlockTime()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should create Digital Dirham wallet on Corda network")
    void shouldCreateDigitalDirhamWallet() throws JsonProcessingException {
        // Given
        var walletRequest = createWalletCreationRequest();
        String createWalletEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/wallets/create";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        headers.set("X-CBDC-Network", "DIGITAL-DIRHAM");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(walletRequest, headers);
        
        // When
        ResponseEntity<CordaTransactionResponse> response = restTemplate.exchange(
            createWalletEndpoint,
            HttpMethod.POST,
            request,
            CordaTransactionResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().transactionId()).isNotNull(),
            () -> assertThat(response.getBody().walletAddress()).isNotNull(),
            () -> assertThat(response.getBody().networkConfirmations()).isEqualTo(1),
            () -> assertThat(response.getBody().status()).isEqualTo("CONFIRMED")
        );
    }

    @Test
    @DisplayName("Should execute Digital Dirham transfer on Corda network")
    void shouldExecuteDigitalDirhamTransfer() {
        // Given
        DigitalDirhamTransfer transfer = createDigitalDirhamTransfer();
        String transferEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/transfers/execute";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        headers.set("X-CBDC-Transfer-Type", "INSTANT");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<DigitalDirhamTransfer> request = new HttpEntity<>(transfer, headers);
        
        // When
        ResponseEntity<CordaTransactionResponse> response = restTemplate.exchange(
            transferEndpoint,
            HttpMethod.POST,
            request,
            CordaTransactionResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().transactionId()).isNotNull(),
            () -> assertThat(response.getBody().blockHash()).isNotNull(),
            () -> assertThat(response.getBody().gasUsed()).isNotNull(),
            () -> assertThat(response.getBody().finalityStatus()).isEqualTo("FINALIZED")
        );
    }

    @Test
    @DisplayName("Should validate wallet balance on Corda network")
    void shouldValidateWalletBalance() {
        // Given
        String walletId = "WALLET-" + UUID.randomUUID().toString();
        String balanceEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/wallets/" + walletId + "/balance";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            balanceEndpoint,
            HttpMethod.GET,
            request,
            Map.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsKey("balance"),
            () -> assertThat(response.getBody()).containsKey("currency"),
            () -> assertThat(response.getBody()).containsKey("lastUpdated"),
            () -> assertThat(response.getBody().get("currency")).isEqualTo("AED-CBDC")
        );
    }

    @Test
    @DisplayName("Should execute cross-border CBDC settlement")
    void shouldExecuteCrossBorderSettlement() {
        // Given
        var crossBorderSettlement = createCrossBorderSettlement();
        String settlementEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/settlements/cross-border";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        headers.set("X-CBDC-Settlement-Network", "GCC-CBDC-BRIDGE");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(crossBorderSettlement, headers);
        
        // When
        ResponseEntity<CordaTransactionResponse> response = restTemplate.exchange(
            settlementEndpoint,
            HttpMethod.POST,
            request,
            CordaTransactionResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().settlementId()).isNotNull(),
            () -> assertThat(response.getBody().bridgeTransactionId()).isNotNull(),
            () -> assertThat(response.getBody().estimatedSettlementTime()).isNotNull()
        );
    }

    @Test
    @DisplayName("Should execute Islamic Finance smart contract on Corda")
    void shouldExecuteIslamicFinanceSmartContract() {
        // Given
        var smartContractRequest = createIslamicFinanceSmartContract();
        String contractEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/contracts/islamic-finance/execute";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        headers.set("X-CBDC-Contract-Type", "MURABAHA_SETTLEMENT");
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(smartContractRequest, headers);
        
        // When
        ResponseEntity<CordaTransactionResponse> response = restTemplate.exchange(
            contractEndpoint,
            HttpMethod.POST,
            request,
            CordaTransactionResponse.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody().success()).isTrue(),
            () -> assertThat(response.getBody().contractAddress()).isNotNull(),
            () -> assertThat(response.getBody().executionResult()).isEqualTo("SUCCESS"),
            () -> assertThat(response.getBody().shariaCompliant()).isTrue()
        );
    }

    @Test
    @DisplayName("Should retrieve network consensus information")
    void shouldRetrieveNetworkConsensusInfo() {
        // Given
        String consensusEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/network/consensus";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            consensusEndpoint,
            HttpMethod.GET,
            request,
            Map.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsKey("consensusNodes"),
            () -> assertThat(response.getBody()).containsKey("notaryNodes"),
            () -> assertThat(response.getBody()).containsKey("lastConsensusTime"),
            () -> assertThat(response.getBody()).containsKey("networkHealth")
        );
    }

    @Test
    @DisplayName("Should validate transaction history on Corda network")
    void shouldValidateTransactionHistory() {
        // Given
        String walletId = "WALLET-" + UUID.randomUUID().toString();
        String historyEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/wallets/" + walletId + "/history";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", createTestAuthToken());
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        headers.add("X-CBDC-History-Limit", "50");
        
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        // When
        ResponseEntity<Map> response = restTemplate.exchange(
            historyEndpoint,
            HttpMethod.GET,
            request,
            Map.class
        );
        
        // Then
        assertAll(
            () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(response.getBody()).isNotNull(),
            () -> assertThat(response.getBody()).containsKey("transactions"),
            () -> assertThat(response.getBody()).containsKey("totalCount"),
            () -> assertThat(response.getBody()).containsKey("pageSize"),
            () -> assertThat(response.getBody()).containsKey("hasMore")
        );
    }

    @Test
    @DisplayName("Should handle Corda network authentication")
    void shouldHandleCordaNetworkAuthentication() {
        // Given
        String protectedEndpoint = getCbdcApiUrl() + CORDA_API_PATH + "/node/identity";
        
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
        headers.set("X-CBDC-Institution-ID", "AMANAHFI-001");
        
        HttpEntity<Void> requestWithAuth = new HttpEntity<>(headers);
        ResponseEntity<NetworkNodeInfo> responseWithAuth = restTemplate.exchange(
            protectedEndpoint,
            HttpMethod.GET,
            requestWithAuth,
            NetworkNodeInfo.class
        );
        
        // Then - Should succeed
        assertAll(
            () -> assertThat(responseWithAuth.getStatusCode()).isEqualTo(HttpStatus.OK),
            () -> assertThat(responseWithAuth.getBody()).isNotNull(),
            () -> assertThat(responseWithAuth.getBody().nodeId()).isNotNull(),
            () -> assertThat(responseWithAuth.getBody().networkId()).isEqualTo("DIGITAL-DIRHAM-NETWORK")
        );
    }

    private Map<String, Object> createWalletCreationRequest() {
        return Map.of(
            "walletType", "INSTITUTIONAL",
            "institutionId", "AMANAHFI-001",
            "accountHolder", Map.of(
                "name", "AmanahFi Platform Digital Treasury",
                "type", "CORPORATE_ENTITY",
                "jurisdiction", "DIFC",
                "registrationNumber", "DIFC-001-2024"
            ),
            "walletFeatures", Map.of(
                "multiSignature", true,
                "signatoryCount", 3,
                "requiredSignatures", 2,
                "dailyLimit", new BigDecimal("10000000.00"),
                "transactionLimit", new BigDecimal("1000000.00"),
                "crossBorderEnabled", true,
                "islamicFinanceCompliant", true
            ),
            "signatories", new Object[]{
                Map.of(
                    "name", "Ahmed Al-Rashid",
                    "role", "CEO",
                    "publicKey", "0x04a1b2c3d4e5f6789012345678901234567890ab",
                    "kycVerified", true
                ),
                Map.of(
                    "name", "Fatima Al-Zahra", 
                    "role", "CFO",
                    "publicKey", "0x04b2c3d4e5f6789012345678901234567890abc1",
                    "kycVerified", true
                ),
                Map.of(
                    "name", "Omar Al-Mansouri",
                    "role", "CTO",
                    "publicKey", "0x04c3d4e5f6789012345678901234567890abcd12",
                    "kycVerified", true
                )
            },
            "complianceSettings", Map.of(
                "amlScreening", true,
                "sanctionsCheck", true,
                "pepScreening", true,
                "transactionMonitoring", true,
                "regulatoryReporting", true
            ),
            "networkSettings", Map.of(
                "networkId", "DIGITAL-DIRHAM-NETWORK",
                "notaryPreference", "CBUAE-NOTARY",
                "consensusParticipation", true,
                "replicationFactor", 3
            )
        );
    }

    private DigitalDirhamTransfer createDigitalDirhamTransfer() {
        return new DigitalDirhamTransfer(
            UUID.randomUUID().toString(),
            "AMANAHFI-001",
            Map.of(
                "walletId", "WALLET-SENDER-" + UUID.randomUUID().toString(),
                "institutionId", "AMANAHFI-001",
                "accountName", "AmanahFi Treasury Account"
            ),
            Map.of(
                "walletId", "WALLET-RECEIVER-" + UUID.randomUUID().toString(),
                "institutionId", "ADCB-CBDC-001",
                "accountName", "ADCB Corporate Treasury"
            ),
            Map.of(
                "amount", new BigDecimal("500000.00"),
                "currency", "AED-CBDC",
                "denomination", "FILS"
            ),
            Map.of(
                "transferType", "INSTITUTIONAL_SETTLEMENT",
                "priority", "HIGH",
                "settlementDate", LocalDateTime.now().toString(),
                "purpose", "ISLAMIC_FINANCE_SETTLEMENT",
                "reference", "MUR-CONTRACT-2024-001"
            ),
            Map.of(
                "encryptionEnabled", true,
                "digitalSignature", true,
                "fraudDetection", true,
                "complianceValidation", true
            ),
            LocalDateTime.now()
        );
    }

    private Map<String, Object> createCrossBorderSettlement() {
        return Map.of(
            "settlementId", UUID.randomUUID().toString(),
            "settlementType", "GCC_CROSS_BORDER",
            "corridor", "UAE_TO_SAUDI",
            "originatingBank", Map.of(
                "institutionId", "AMANAHFI-001",
                "country", "AE",
                "cbdcWallet", "WALLET-UAE-" + UUID.randomUUID().toString(),
                "swiftCode", "AMAHAEAXXXX"
            ),
            "receivingBank", Map.of(
                "institutionId", "RIYAD-BANK-001",
                "country", "SA",
                "cbdcWallet", "WALLET-SAU-" + UUID.randomUUID().toString(),
                "swiftCode", "RIBLSARIXXX"
            ),
            "settlement", Map.of(
                "amount", new BigDecimal("2500000.00"),
                "sourceCurrency", "AED-CBDC",
                "targetCurrency", "SAR-CBDC",
                "exchangeRate", new BigDecimal("1.0204"),
                "settledAmount", new BigDecimal("2551000.00"),
                "bridgeProtocol", "GCC-CBDC-BRIDGE-V2"
            ),
            "underlyingTransaction", Map.of(
                "transactionType", "TRADE_SETTLEMENT",
                "tradeReference", "TRADE-2024-GCC-789",
                "commodity", "HALAL_FOOD_PRODUCTS",
                "documentaryCredit", "LC-2024-456789",
                "islamicFinanceStructure", "MURABAHA"
            ),
            "regulatoryCompliance", Map.of(
                "fatfTravelRule", true,
                "sanctions screening", "CLEAR",
                "crsReporting", true,
                "originatingCountryReporting", true,
                "receivingCountryReporting", true
            ),
            "bridgeNetwork", Map.of(
                "networkType", "WHOLESALE_CBDC",
                "bridgeOperator", "GCC-MONETARY-COUNCIL",
                "settlementWindow", "T+0",
                "finalityGuarantee", "IRREVOCABLE"
            ),
            "smartContractExecution", Map.of(
                "contractAddress", "0x987654321abcdef1234567890",
                "automatedSettlement", true,
                "conditionsValidation", true,
                "complianceChecks", true
            )
        );
    }

    private Map<String, Object> createIslamicFinanceSmartContract() {
        return Map.of(
            "contractId", UUID.randomUUID().toString(),
            "contractType", "MURABAHA_PAYMENT_SETTLEMENT",
            "islamicFinanceStructure", "MURABAHA",
            "contractTerms", Map.of(
                "asset", Map.of(
                    "description", "Commercial Real Estate in Business Bay",
                    "value", new BigDecimal("5000000.00"),
                    "currency", "AED",
                    "shariaCompliant", true
                ),
                "financingDetails", Map.of(
                    "costPrice", new BigDecimal("5000000.00"),
                    "profitAmount", new BigDecimal("500000.00"),
                    "sellingPrice", new BigDecimal("5500000.00"),
                    "profitRate", 0.10,
                    "paymentSchedule", "60_MONTHS"
                ),
                "parties", Map.of(
                    "financier", Map.of(
                        "name", "AmanahFi Platform",
                        "walletAddress", "WALLET-FINANCIER-" + UUID.randomUUID().toString(),
                        "role", "SELLER"
                    ),
                    "customer", Map.of(
                        "name", "Dubai Investment Holdings",
                        "walletAddress", "WALLET-CUSTOMER-" + UUID.randomUUID().toString(),
                        "role", "BUYER"
                    )
                )
            ),
            "paymentInstruction", Map.of(
                "installmentAmount", new BigDecimal("91666.67"),
                "installmentNumber", 12,
                "dueDate", LocalDateTime.now().plusDays(30).toString(),
                "paymentMethod", "CBDC_TRANSFER",
                "automaticExecution", true
            ),
            "complianceValidation", Map.of(
                "shariaComplianceCheck", true,
                "hsaValidation", "APPROVED",
                "assetOwnershipVerified", true,
                "transparentPricing", true,
                "riskSharingCompliant", true
            ),
            "executionParameters", Map.of(
                "gasLimit", 500000,
                "gasPrice", new BigDecimal("0.00001"),
                "executionPriority", "HIGH",
                "conditionalExecution", Map.of(
                    "customerWalletBalance", "SUFFICIENT",
                    "complianceStatus", "VALIDATED",
                    "networkConsensus", "ACHIEVED"
                )
            ),
            "auditTrail", Map.of(
                "hsaApprovalRef", "HSA-2024-MUR-001",
                "legalDocumentHash", "0xabcdef123456789",
                "executionWitnesses", new String[]{
                    "CBUAE-NOTARY-001",
                    "DIFC-NOTARY-002"
                },
                "regulatoryReporting", true
            )
        );
    }
}