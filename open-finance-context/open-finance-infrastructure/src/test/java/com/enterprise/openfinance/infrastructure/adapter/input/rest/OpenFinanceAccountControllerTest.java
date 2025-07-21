package com.enterprise.openfinance.infrastructure.adapter.input.rest;

import com.enterprise.openfinance.application.saga.DataSharingRequestSaga;
import com.enterprise.openfinance.application.saga.model.*;
import com.enterprise.openfinance.domain.model.consent.ConsentId;
import com.enterprise.openfinance.domain.model.consent.ConsentScope;
import com.enterprise.openfinance.domain.model.participant.ParticipantId;
import com.enterprise.openfinance.domain.port.input.AccountInformationUseCase;
import com.enterprise.shared.domain.CustomerId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD tests for OpenFinanceAccountController.
 * Tests the Open Finance account information APIs with comprehensive scenarios
 * including cross-platform data aggregation, FAPI 2.0 security, and saga orchestration.
 */
@WebMvcTest(OpenFinanceAccountController.class)
@Tag("api")
@Tag("tdd")
@Tag("open-finance")
@DisplayName("Open Finance Account Controller TDD Tests")
class OpenFinanceAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountInformationUseCase accountInformationUseCase;

    @MockBean
    private DataSharingRequestSaga dataSharingRequestSaga;

    @MockBean
    private OpenFinanceSecurityValidator securityValidator;

    @MockBean
    private ConsentValidator consentValidator;

    // Test data
    private static final String VALID_CONSENT_ID = "CONSENT-ABC12345";
    private static final String VALID_PARTICIPANT_ID = "BANK-TEST01";
    private static final String VALID_CUSTOMER_ID = "CUSTOMER-789";
    private static final String VALID_DPOP_TOKEN = "eyJ0eXAiOiJkcG9wK2p3dCIsImFsZyI6IlJTMjU2In0...";

    @BeforeEach
    void setUp() {
        // Setup default successful validations
        when(securityValidator.validateFAPI2Request(anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(SecurityValidationResult.valid()));
        
        when(consentValidator.validateConsentForAccounts(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(ConsentValidationResult.valid()));
    }

    // === TDD: Red-Green-Refactor for Account Information API ===

    @Test
    @DisplayName("RED: Given account request without proper headers, When calling API, Then should return 400")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_400_when_required_headers_missing() throws Exception {
        // Given: Request without required headers
        
        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .contentType(MediaType.APPLICATION_JSON))
        
        // Then: Should return 400 Bad Request
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GREEN: Given valid request, When calling accounts API, Then should return aggregated account data")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_aggregated_account_data_successfully() throws Exception {
        // Given: Valid request with successful saga execution
        var mockDataSharingResult = DataSharingResult.builder()
            .sagaId(SagaId.generate())
            .requestId(DataRequestId.generate())
            .consentId(ConsentId.of(VALID_CONSENT_ID))
            .customerId(CustomerId.of(VALID_CUSTOMER_ID))
            .participantId(ParticipantId.of(VALID_PARTICIPANT_ID))
            .status(DataSharingStatus.COMPLETED)
            .sharedAt(Instant.now())
            .dataSources(List.of("ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK"))
            .dataSize(1024L)
            .executionTimeMs(2500L)
            .aggregatedData(createMockAggregatedData())
            .build();

        when(dataSharingRequestSaga.orchestrateDataSharingRequest(any()))
            .thenReturn(CompletableFuture.completedFuture(mockDataSharingResult));

        // When: Calling accounts API with valid headers
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 200 with aggregated account data
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.accounts").isArray())
                .andExpect(jsonPath("$.data.totalAccounts").isNumber())
                .andExpect(jsonPath("$.meta.requestId").exists())
                .andExpect(jsonPath("$.meta.platforms").isArray())
                .andExpected(jsonPath("$.meta.platforms", containsInAnyOrder(
                    "ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK")))
                .andExpect(jsonPath("$.links.self").value("/open-finance/v1/accounts"));

        // And: Should have called saga orchestration
        verify(dataSharingRequestSaga).orchestrateDataSharingRequest(argThat(request -> 
            request.getConsentId().getValue().equals(VALID_CONSENT_ID) &&
            request.getCustomerId().getValue().equals(VALID_CUSTOMER_ID) &&
            request.getParticipantId().getValue().equals(VALID_PARTICIPANT_ID) &&
            request.getRequestedScopes().contains(ConsentScope.ACCOUNT_INFORMATION)
        ));
    }

    @Test
    @DisplayName("REFACTOR: Given FAPI security validation failure, When calling API, Then should return 401")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_401_on_fapi_security_validation_failure() throws Exception {
        // Given: FAPI security validation fails
        when(securityValidator.validateFAPI2Request(anyString(), anyString()))
            .thenReturn(CompletableFuture.completedFuture(
                SecurityValidationResult.invalid("Invalid DPoP token signature")));

        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", "invalid-dpop-token")
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized())
                .andExpected(jsonPath("$.error.message", containsString("Invalid security validation")));

        // And: Should not call data sharing saga
        verify(dataSharingRequestSaga, never()).orchestrateDataSharingRequest(any());
    }

    // === TDD: Consent Validation Tests ===

    @Test
    @DisplayName("Given expired consent, When calling API, Then should return 403 Forbidden")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_403_on_expired_consent() throws Exception {
        // Given: Consent validation fails due to expiration
        when(consentValidator.validateConsentForAccounts(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(
                ConsentValidationResult.invalid(Set.of("CONSENT_EXPIRED"))));

        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 403 Forbidden
                .andExpect(status().isForbidden())
                .andExpected(jsonPath("$.error.message", containsString("Invalid consent")))
                .andExpected(jsonPath("$.error.message", containsString("CONSENT_EXPIRED")));
    }

    @Test
    @DisplayName("Given insufficient consent scope, When calling API, Then should return 403")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_403_on_insufficient_consent_scope() throws Exception {
        // Given: Consent validation fails due to insufficient scope
        when(consentValidator.validateConsentForAccounts(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(
                ConsentValidationResult.invalid(Set.of("INSUFFICIENT_SCOPE"))));

        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 403 Forbidden
                .andExpect(status().isForbidden())
                .andExpected(jsonPath("$.error.message", containsString("INSUFFICIENT_SCOPE")));
    }

    // === TDD: Cross-Platform Data Aggregation Tests ===

    @Test
    @DisplayName("Given saga data sharing failure, When calling API, Then should return 500 with error details")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_500_on_saga_data_sharing_failure() throws Exception {
        // Given: Data sharing saga fails
        var mockFailureResult = DataSharingResult.builder()
            .sagaId(SagaId.generate())
            .requestId(DataRequestId.generate())
            .consentId(ConsentId.of(VALID_CONSENT_ID))
            .customerId(CustomerId.of(VALID_CUSTOMER_ID))
            .participantId(ParticipantId.of(VALID_PARTICIPANT_ID))
            .status(DataSharingStatus.FAILED)
            .failureReason("Enterprise loan service unavailable")
            .failedAt(Instant.now())
            .build();

        when(dataSharingRequestSaga.orchestrateDataSharingRequest(any()))
            .thenReturn(CompletableFuture.completedFuture(mockFailureResult));

        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 500 Internal Server Error
                .andExpected(status().isInternalServerError())
                .andExpected(jsonPath("$.error.message", containsString("Data retrieval failed")))
                .andExpected(jsonPath("$.error.message", containsString("Enterprise loan service unavailable")));
    }

    @Test
    @DisplayName("Given saga timeout, When calling API, Then should return 500 with timeout message")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_500_on_saga_timeout() throws Exception {
        // Given: Data sharing saga times out
        var mockTimeoutResult = DataSharingResult.builder()
            .sagaId(SagaId.generate())
            .requestId(DataRequestId.generate())
            .consentId(ConsentId.of(VALID_CONSENT_ID))
            .customerId(CustomerId.of(VALID_CUSTOMER_ID))
            .participantId(ParticipantId.of(VALID_PARTICIPANT_ID))
            .status(DataSharingStatus.TIMEOUT)
            .failureReason("Data aggregation timeout after 2 minutes")
            .failedAt(Instant.now())
            .build();

        when(dataSharingRequestSaga.orchestrateDataSharingRequest(any()))
            .thenReturn(CompletableFuture.completedFuture(mockTimeoutResult));

        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 500 with timeout information
                .andExpected(status().isInternalServerError())
                .andExpected(jsonPath("$.error.message", containsString("timeout")));
    }

    // === TDD: Financial Summary API Tests ===

    @Test
    @DisplayName("Given comprehensive scope request, When calling financial summary, Then should aggregate all platforms")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION", "SCOPE_SPENDING_ANALYSIS"})
    void should_return_comprehensive_financial_summary() throws Exception {
        // Given: Successful comprehensive data aggregation
        var mockSummaryResult = DataSharingResult.builder()
            .sagaId(SagaId.generate())
            .status(DataSharingStatus.COMPLETED)
            .dataSources(List.of("ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK"))
            .aggregatedData(createMockFinancialSummaryData())
            .build();

        when(dataSharingRequestSaga.orchestrateDataSharingRequest(any()))
            .thenReturn(CompletableFuture.completedFuture(mockSummaryResult));

        // When: Calling financial summary API
        mockMvc.perform(get("/open-finance/v1/accounts/summary")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return comprehensive financial summary
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAssets").exists())
                .andExpect(jsonPath("$.data.totalLiabilities").exists())
                .andExpected(jsonPath("$.data.netWorth").exists())
                .andExpected(jsonPath("$.data.monthlyIncome").exists())
                .andExpected(jsonPath("$.data.monthlyExpenses").exists())
                .andExpected(jsonPath("$.data.monthlySurplus").exists())
                .andExpected(jsonPath("$.meta.platforms", hasSize(3)));

        // And: Should request comprehensive scopes
        verify(dataSharingRequestSaga).orchestrateDataSharingRequest(argThat(request -> 
            request.getRequestedScopes().contains(ConsentScope.ACCOUNT_INFORMATION) &&
            request.getRequestedScopes().contains(ConsentScope.LOAN_INFORMATION) &&
            request.getRequestedScopes().contains(ConsentScope.ISLAMIC_FINANCE) &&
            request.getRequestedScopes().contains(ConsentScope.SPENDING_ANALYSIS)
        ));
    }

    // === TDD: Health Check Tests ===

    @Test
    @DisplayName("Given health check request, When calling health endpoint, Then should return service status")
    void should_return_health_status() throws Exception {
        // When: Calling health endpoint
        mockMvc.perform(get("/open-finance/v1/accounts/health")
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return health status
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.services").exists())
                .andExpect(jsonPath("$.services['enterprise-loans']").value("UP"))
                .andExpect(jsonPath("$.services['amanahfi-platform']").value("UP"))
                .andExpect(jsonPath("$.services['masrufi-framework']").value("UP"))
                .andExpect(jsonPath("$.services['consent-validation']").value("UP"));
    }

    // === TDD: Request Validation Tests ===

    @Test
    @DisplayName("Given invalid consent ID format, When calling API, Then should return 400")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_400_on_invalid_consent_id_format() throws Exception {
        // Given: Invalid consent ID format
        var invalidConsentId = "INVALID-CONSENT";

        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", invalidConsentId)
                .header("X-Participant-Id", VALID_PARTICIPANT_ID)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 400 Bad Request for validation error
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given invalid participant ID format, When calling API, Then should return 400")
    @WithMockUser(authorities = {"SCOPE_ACCOUNT_INFORMATION"})
    void should_return_400_on_invalid_participant_id_format() throws Exception {
        // Given: Invalid participant ID format
        var invalidParticipantId = "INVALID-PARTICIPANT";

        // When: Calling accounts API
        mockMvc.perform(get("/open-finance/v1/accounts")
                .header("X-Consent-Id", VALID_CONSENT_ID)
                .header("X-Participant-Id", invalidParticipantId)
                .header("X-Customer-Id", VALID_CUSTOMER_ID)
                .header("DPoP", VALID_DPOP_TOKEN)
                .contentType(MediaType.APPLICATION_JSON))

        // Then: Should return 400 Bad Request
                .andExpected(status().isBadRequest());
    }

    // === Helper methods for test data creation ===

    private AggregatedData createMockAggregatedData() {
        return AggregatedData.builder()
            .aggregationId(AggregationId.generate())
            .platformDataList(List.of(
                createMockPlatformData("ENTERPRISE_LOANS"),
                createMockPlatformData("AMANAHFI_PLATFORM"),
                createMockPlatformData("MASRUFI_FRAMEWORK")
            ))
            .sourceCount(3)
            .dataSize(2048L)
            .dataSources(List.of("ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK"))
            .aggregatedAt(Instant.now())
            .build();
    }

    private AggregatedData createMockFinancialSummaryData() {
        return AggregatedData.builder()
            .aggregationId(AggregationId.generate())
            .platformDataList(List.of(
                createMockFinancialPlatformData("ENTERPRISE_LOANS", 250000, 180000),
                createMockFinancialPlatformData("AMANAHFI_PLATFORM", 150000, 100000),
                createMockFinancialPlatformData("MASRUFI_FRAMEWORK", 50000, 20000)
            ))
            .sourceCount(3)
            .dataSize(4096L)
            .dataSources(List.of("ENTERPRISE_LOANS", "AMANAHFI_PLATFORM", "MASRUFI_FRAMEWORK"))
            .aggregatedAt(Instant.now())
            .build();
    }

    private PlatformData createMockPlatformData(String platformName) {
        return PlatformData.builder()
            .sourcePlatform(platformName)
            .dataSize(512L)
            .accountData(List.of(
                Map.of("accountId", platformName + "-ACC-001", "balance", 10000),
                Map.of("accountId", platformName + "-ACC-002", "balance", 25000)
            ))
            .retrievedAt(Instant.now())
            .build();
    }

    private PlatformData createMockFinancialPlatformData(String platformName, int assets, int liabilities) {
        return PlatformData.builder()
            .sourcePlatform(platformName)
            .dataSize(1024L)
            .financialSummary(Map.of(
                "totalAssets", assets,
                "totalLiabilities", liabilities,
                "monthlyIncome", 15000,
                "monthlyExpenses", 12000
            ))
            .retrievedAt(Instant.now())
            .build();
    }
}