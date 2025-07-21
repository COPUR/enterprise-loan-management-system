package com.enterprise.openfinance.infrastructure.adapter.output.cbuae;

import com.enterprise.openfinance.domain.model.participant.Participant;
import com.enterprise.openfinance.domain.model.participant.ParticipantId;
import com.enterprise.openfinance.domain.port.output.CBUAEIntegrationPort;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * TDD integration tests for CBUAE Directory Adapter.
 * Tests the integration with CBUAE APIs using WireMock.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CBUAEDirectoryAdapterTest {

    private static WireMockServer cbuaeMockServer;
    private CBUAEDirectoryAdapter adapter;

    @BeforeAll
    static void setupWireMock() {
        cbuaeMockServer = new WireMockServer(8089);
        cbuaeMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterAll
    static void tearDownWireMock() {
        cbuaeMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("open-finance.cbuae.api-base-url", 
                () -> "http://localhost:" + cbuaeMockServer.port());
    }

    @BeforeEach
    void setUp() {
        cbuaeMockServer.resetAll();
        
        var webClient = WebClient.builder()
                .baseUrl("http://localhost:" + cbuaeMockServer.port())
                .build();
                
        adapter = new CBUAEDirectoryAdapter(webClient);
    }

    // === TDD: Red-Green-Refactor for Participant Directory Sync ===

    @Test
    @DisplayName("Given CBUAE directory with participants, When syncing directory, Then should return all participants")
    void should_sync_participant_directory_successfully() {
        // Given: CBUAE directory with participants
        stubFor(get(urlEqualTo("/participants"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "participants": [
                                    {
                                        "id": "BANK-001",
                                        "legalName": "Emirates NBD Bank PJSC",
                                        "role": "DATA_HOLDER",
                                        "status": "ACTIVE",
                                        "registration": {
                                            "registrationId": "REG-BANK-001",
                                            "licenseNumber": "LIC-001",
                                            "registeredAt": "2024-01-01T00:00:00Z",
                                            "status": "ACTIVE"
                                        }
                                    },
                                    {
                                        "id": "FINTECH-001",
                                        "legalName": "Test Fintech LLC",
                                        "role": "DATA_RECIPIENT",
                                        "status": "ACTIVE",
                                        "registration": {
                                            "registrationId": "REG-FINTECH-001",
                                            "licenseNumber": "LIC-002",
                                            "registeredAt": "2024-01-15T00:00:00Z",
                                            "status": "ACTIVE"
                                        }
                                    }
                                ]
                            }
                        """)));

        // When: Syncing participant directory
        var participants = adapter.syncParticipantDirectory();

        // Then: Should return all participants
        assertThat(participants).hasSize(2);
        
        var bank = participants.stream()
                .filter(p -> p.getId().getValue().equals("BANK-001"))
                .findFirst()
                .orElseThrow();
        assertThat(bank.getLegalName()).isEqualTo("Emirates NBD Bank PJSC");
        
        var fintech = participants.stream()
                .filter(p -> p.getId().getValue().equals("FINTECH-001"))
                .findFirst()
                .orElseThrow();
        assertThat(fintech.getLegalName()).isEqualTo("Test Fintech LLC");
        
        // And: Should have made correct API call
        verify(getRequestedFor(urlEqualTo("/participants")));
    }

    @Test
    @DisplayName("Given CBUAE API unavailable, When syncing directory, Then should throw exception")
    void should_handle_cbuae_api_unavailable() {
        // Given: CBUAE API is unavailable
        stubFor(get(urlEqualTo("/participants"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "SERVICE_UNAVAILABLE",
                                "message": "CBUAE services are currently unavailable"
                            }
                        """)));

        // When & Then: Should throw CBUAEIntegrationException
        assertThatThrownBy(() -> adapter.syncParticipantDirectory())
                .isInstanceOf(CBUAEIntegrationPort.CBUAEIntegrationException.class)
                .hasMessageContaining("Failed to sync participant directory")
                .extracting("errorCode")
                .isEqualTo("SERVICE_UNAVAILABLE");
    }

    // === TDD: Individual Participant Retrieval ===

    @Test
    @DisplayName("Given valid participant ID, When getting participant, Then should return participant")
    void should_get_participant_successfully() {
        // Given: Valid participant in CBUAE directory
        var participantId = ParticipantId.of("BANK-001");
        
        stubFor(get(urlEqualTo("/participants/BANK-001"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "id": "BANK-001",
                                "legalName": "Emirates NBD Bank PJSC",
                                "role": "DATA_HOLDER",
                                "status": "ACTIVE",
                                "registration": {
                                    "registrationId": "REG-BANK-001",
                                    "licenseNumber": "LIC-001",
                                    "registeredAt": "2024-01-01T00:00:00Z",
                                    "status": "ACTIVE"
                                },
                                "certificates": [
                                    {
                                        "serialNumber": "123456789",
                                        "issuer": "CBUAE Root CA",
                                        "subject": "CN=Emirates NBD Bank",
                                        "validFrom": "2024-01-01T00:00:00Z",
                                        "validTo": "2025-01-01T00:00:00Z",
                                        "status": "ACTIVE"
                                    }
                                ]
                            }
                        """)));

        // When: Getting participant
        var participantOpt = adapter.getParticipant(participantId);

        // Then: Should return participant
        assertThat(participantOpt).isPresent();
        
        var participant = participantOpt.get();
        assertThat(participant.getId()).isEqualTo(participantId);
        assertThat(participant.getLegalName()).isEqualTo("Emirates NBD Bank PJSC");
        assertThat(participant.getCertificates()).hasSize(1);
        
        // And: Should have made correct API call
        verify(getRequestedFor(urlEqualTo("/participants/BANK-001")));
    }

    @Test
    @DisplayName("Given non-existent participant ID, When getting participant, Then should return empty")
    void should_return_empty_for_non_existent_participant() {
        // Given: Non-existent participant ID
        var participantId = ParticipantId.of("NON-EXISTENT");
        
        stubFor(get(urlEqualTo("/participants/NON-EXISTENT"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "PARTICIPANT_NOT_FOUND",
                                "message": "Participant not found in directory"
                            }
                        """)));

        // When: Getting participant
        var participantOpt = adapter.getParticipant(participantId);

        // Then: Should return empty
        assertThat(participantOpt).isEmpty();
        
        // And: Should have made correct API call
        verify(getRequestedFor(urlEqualTo("/participants/NON-EXISTENT")));
    }

    // === TDD: Participant Validation ===

    @Test
    @DisplayName("Given active participant, When validating, Then should return true")
    void should_validate_active_participant() {
        // Given: Active participant in CBUAE directory
        var participantId = ParticipantId.of("BANK-001");
        
        stubFor(get(urlEqualTo("/participants/BANK-001/validate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "valid": true,
                                "status": "ACTIVE",
                                "lastValidated": "2024-01-20T10:00:00Z"
                            }
                        """)));

        // When: Validating participant
        var isValid = adapter.validateParticipant(participantId);

        // Then: Should return true
        assertThat(isValid).isTrue();
        
        // And: Should have made correct API call
        verify(getRequestedFor(urlEqualTo("/participants/BANK-001/validate")));
    }

    @Test
    @DisplayName("Given suspended participant, When validating, Then should return false")
    void should_invalidate_suspended_participant() {
        // Given: Suspended participant in CBUAE directory
        var participantId = ParticipantId.of("SUSPENDED-001");
        
        stubFor(get(urlEqualTo("/participants/SUSPENDED-001/validate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "valid": false,
                                "status": "SUSPENDED",
                                "reason": "Regulatory violation",
                                "lastValidated": "2024-01-20T10:00:00Z"
                            }
                        """)));

        // When: Validating participant
        var isValid = adapter.validateParticipant(participantId);

        // Then: Should return false
        assertThat(isValid).isFalse();
    }

    // === TDD: API Registration ===

    @Test
    @DisplayName("Given valid API specification, When registering APIs, Then should return registration ID")
    void should_register_apis_successfully() {
        // Given: Valid OpenAPI specification
        var apiSpec = """
            {
                "openapi": "3.0.0",
                "info": {
                    "title": "Open Finance API",
                    "version": "1.0.0"
                },
                "paths": {
                    "/accounts": {
                        "get": {
                            "summary": "List accounts"
                        }
                    }
                }
            }
        """;
        
        stubFor(post(urlEqualTo("/api-registry"))
                .withRequestBody(containing("Open Finance API"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "registrationId": "REG-API-12345",
                                "status": "REGISTERED",
                                "registeredAt": "2024-01-20T10:00:00Z"
                            }
                        """)));

        // When: Registering APIs
        var registrationId = adapter.registerAPIs(apiSpec);

        // Then: Should return registration ID
        assertThat(registrationId).isEqualTo("REG-API-12345");
        
        // And: Should have made correct API call
        verify(postRequestedFor(urlEqualTo("/api-registry"))
                .withRequestBody(containing("Open Finance API")));
    }

    @Test
    @DisplayName("Given invalid API specification, When registering APIs, Then should throw exception")
    void should_handle_invalid_api_specification() {
        // Given: Invalid OpenAPI specification
        var invalidSpec = "invalid json";
        
        stubFor(post(urlEqualTo("/api-registry"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "error": "INVALID_API_SPEC",
                                "message": "Invalid OpenAPI specification format"
                            }
                        """)));

        // When & Then: Should throw exception
        assertThatThrownBy(() -> adapter.registerAPIs(invalidSpec))
                .isInstanceOf(CBUAEIntegrationPort.CBUAEIntegrationException.class)
                .hasMessageContaining("Failed to register APIs")
                .extracting("errorCode")
                .isEqualTo("INVALID_API_SPEC");
    }

    // === TDD: Health Check ===

    @Test
    @DisplayName("Given healthy CBUAE service, When checking health, Then should return healthy status")
    void should_return_healthy_status() {
        // Given: Healthy CBUAE service
        stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "status": "UP",
                                "responseTime": 150,
                                "lastUpdate": "2024-01-20T10:00:00Z",
                                "services": {
                                    "directory": "UP",
                                    "certificates": "UP",
                                    "sandbox": "UP"
                                }
                            }
                        """)));

        // When: Checking health status
        var healthStatus = adapter.getHealthStatus();

        // Then: Should return healthy status
        assertThat(healthStatus.isAvailable()).isTrue();
        assertThat(healthStatus.status()).isEqualTo("UP");
        assertThat(healthStatus.responseTimeMs()).isEqualTo(150);
        assertThat(healthStatus.lastUpdateTime()).isEqualTo("2024-01-20T10:00:00Z");
    }

    @Test
    @DisplayName("Given degraded CBUAE service, When checking health, Then should return degraded status")
    void should_return_degraded_status() {
        // Given: Degraded CBUAE service
        stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                                "status": "DEGRADED",
                                "responseTime": 2500,
                                "lastUpdate": "2024-01-20T10:00:00Z",
                                "services": {
                                    "directory": "UP",
                                    "certificates": "DOWN",
                                    "sandbox": "UP"
                                }
                            }
                        """)));

        // When: Checking health status
        var healthStatus = adapter.getHealthStatus();

        // Then: Should return degraded status
        assertThat(healthStatus.isAvailable()).isTrue(); // Still available but degraded
        assertThat(healthStatus.status()).isEqualTo("DEGRADED");
        assertThat(healthStatus.responseTimeMs()).isEqualTo(2500);
    }

    // === TDD: Error Handling and Resilience ===

    @Test
    @DisplayName("Given network timeout, When calling CBUAE API, Then should throw timeout exception")
    void should_handle_network_timeout() {
        // Given: Network timeout scenario
        stubFor(get(urlEqualTo("/participants"))
                .willReturn(aResponse()
                        .withFixedDelay(30000) // 30 second delay
                        .withStatus(200)));

        // When & Then: Should throw timeout exception
        assertThatThrownBy(() -> adapter.syncParticipantDirectory())
                .isInstanceOf(CBUAEIntegrationPort.CBUAEIntegrationException.class)
                .hasMessageContaining("timeout")
                .extracting("errorCode")
                .isEqualTo("TIMEOUT");
    }

    @Test
    @DisplayName("Given rate limiting, When calling CBUAE API, Then should handle rate limit exception")
    void should_handle_rate_limiting() {
        // Given: Rate limiting response
        stubFor(get(urlEqualTo("/participants/BANK-001"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("Retry-After", "60")
                        .withBody("""
                            {
                                "error": "RATE_LIMIT_EXCEEDED",
                                "message": "Rate limit exceeded. Retry after 60 seconds."
                            }
                        """)));

        // When & Then: Should throw rate limit exception
        assertThatThrownBy(() -> adapter.getParticipant(ParticipantId.of("BANK-001")))
                .isInstanceOf(CBUAEIntegrationPort.CBUAEIntegrationException.class)
                .hasMessageContaining("Rate limit exceeded")
                .extracting("errorCode")
                .isEqualTo("RATE_LIMIT_EXCEEDED");
    }
}