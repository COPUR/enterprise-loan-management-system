package com.loanmanagement.security.zerotrust;

import com.loanmanagement.security.zerotrust.domain.model.*;
import com.loanmanagement.security.zerotrust.domain.ports.inbound.ZeroTrustSecurityUseCase;
import com.loanmanagement.security.zerotrust.infrastructure.adapter.inbound.ZeroTrustSecurityController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive integration test suite for Zero Trust Security
 * Verifies hexagonal architecture implementation and performance
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "logging.level.com.loanmanagement.security=DEBUG"
})
@DisplayName("Zero Trust Security Integration Tests")
class ZeroTrustSecurityIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private ZeroTrustSecurityUseCase zeroTrustSecurityUseCase;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Nested
    @DisplayName("Zero Trust Security Validation")
    class ZeroTrustValidationTests {
        
        @Test
        @DisplayName("Should validate security for legitimate user session")
        void shouldValidateSecurityForLegitimateUserSession() throws Exception {
            // Given
            var request = new ZeroTrustSecurityController.ZeroTrustSecurityRequestDto(
                "session-123",
                "user-456",
                "device-789",
                "192.168.1.100",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
                Map.of("userType", "PREMIUM", "location", "OFFICE"),
                "TRANSACTION",
                "resource-001"
            );
            
            // When & Then
            mockMvc.perform(post("/api/v1/security/zero-trust/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.sessionId").value("session-123"));
        }
        
        @Test
        @DisplayName("Should reject security validation for suspicious activity")
        void shouldRejectSecurityValidationForSuspiciousActivity() throws Exception {
            // Given
            var request = new ZeroTrustSecurityController.ZeroTrustSecurityRequestDto(
                "session-suspicious",
                "user-unknown",
                "device-untrusted",
                "192.168.1.999", // Invalid IP
                "Suspicious-Agent/1.0",
                Map.of("anomalyScore", 0.9),
                "ADMINISTRATIVE_ACTION",
                "sensitive-resource"
            );
            
            // When & Then
            mockMvc.perform(post("/api/v1/security/zero-trust/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(greaterThan(0)));
        }
        
        @Test
        @DisplayName("Should handle concurrent validation requests efficiently")
        void shouldHandleConcurrentValidationRequestsEfficiently() {
            // Given
            var startTime = System.currentTimeMillis();
            var requestCount = 100;
            
            // When
            var futures = java.util.stream.IntStream.range(0, requestCount)
                .mapToObj(i -> {
                    var request = new ZeroTrustSecurityRequest(
                        "session-" + i,
                        "user-" + i,
                        "device-" + i,
                        "192.168.1." + (i % 255),
                        "TestAgent/1.0",
                        LocalDateTime.now(),
                        Map.of("requestId", i),
                        SecurityOperation.TRANSACTION,
                        "resource-" + i
                    );
                    return zeroTrustSecurityUseCase.validateSecurity(request);
                })
                .toList();
            
            // Wait for all to complete
            var results = futures.stream()
                .map(future -> {
                    try {
                        return future.get(10, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
            
            var endTime = System.currentTimeMillis();
            var totalTime = endTime - startTime;
            
            // Then
            assertThat(results).hasSize(requestCount);
            assertThat(totalTime).isLessThan(5000); // Should complete within 5 seconds
            assertThat(results.stream().allMatch(r -> r.sessionId() != null)).isTrue();
            
            System.out.printf("Processed %d concurrent validations in %d ms (%.1f/sec)%n", 
                            requestCount, totalTime, (requestCount * 1000.0) / totalTime);
        }
    }
    
    @Nested
    @DisplayName("Continuous Verification")
    class ContinuousVerificationTests {
        
        @Test
        @DisplayName("Should perform continuous verification successfully")
        void shouldPerformContinuousVerificationSuccessfully() throws Exception {
            // Given
            var request = new ZeroTrustSecurityController.ContinuousVerificationRequestDto(
                "session-cv-123",
                "user-cv-456",
                LocalDateTime.now().minusMinutes(15),
                List.of(
                    new ZeroTrustSecurityController.SecurityEventDto(
                        "LOGIN",
                        "User logged in",
                        LocalDateTime.now().minusMinutes(10),
                        Map.of("source", "web")
                    ),
                    new ZeroTrustSecurityController.SecurityEventDto(
                        "TRANSACTION",
                        "Payment initiated",
                        LocalDateTime.now().minusMinutes(5),
                        Map.of("amount", 1000)
                    )
                ),
                Map.of("sessionDuration", "15_MINUTES"),
                "TIME_BASED"
            );
            
            // When & Then
            mockMvc.perform(post("/api/v1/security/zero-trust/continuous-verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationPassed").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.nextVerificationTime").exists());
        }
        
        @Test
        @DisplayName("Should detect behavioral anomalies in continuous verification")
        void shouldDetectBehavioralAnomaliesInContinuousVerification() {
            // Given
            var request = new ContinuousVerificationRequest(
                "session-anomaly",
                "user-anomaly",
                LocalDateTime.now().minusHours(1),
                List.of(
                    new SecurityEvent(
                        "SUSPICIOUS_LOGIN",
                        "Login from unusual location",
                        LocalDateTime.now().minusMinutes(30),
                        Map.of("location", "UNKNOWN")
                    ),
                    new SecurityEvent(
                        "RAPID_TRANSACTIONS",
                        "Multiple rapid transactions",
                        LocalDateTime.now().minusMinutes(10),
                        Map.of("count", 20)
                    )
                ),
                Map.of("riskScore", 0.8),
                VerificationTrigger.BEHAVIOR_CHANGE
            );
            
            // When
            var result = zeroTrustSecurityUseCase.performContinuousVerification(request)
                .join();
            
            // Then
            assertThat(result.verificationPassed()).isFalse();
            assertThat(result.status()).isEqualTo(VerificationStatus.SUSPICIOUS);
            assertThat(result.anomalies()).isNotEmpty();
            assertThat(result.recommendedControls()).isNotEmpty();
        }
    }
    
    @Nested
    @DisplayName("Threat Assessment")
    class ThreatAssessmentTests {
        
        @Test
        @DisplayName("Should assess low threat for normal activity")
        void shouldAssessLowThreatForNormalActivity() {
            // Given
            var request = new ThreatAssessmentRequest(
                "source-normal",
                "resource-safe",
                ThreatType.EXTERNAL_ATTACK,
                List.of(),
                Map.of("sourceReputation", "GOOD"),
                LocalDateTime.now()
            );
            
            // When
            var result = zeroTrustSecurityUseCase.assessThreat(request).join();
            
            // Then
            assertThat(result.threatLevel()).isIn(ThreatLevel.NONE, ThreatLevel.LOW);
            assertThat(result.identifiedThreats()).isEmpty();
            assertThat(result.riskScore()).isLessThan(java.math.BigDecimal.valueOf(0.3));
        }
        
        @Test
        @DisplayName("Should assess high threat for malicious indicators")
        void shouldAssessHighThreatForMaliciousIndicators() {
            // Given
            var maliciousIndicators = List.of(
                new ThreatIndicator(
                    "IP_REPUTATION",
                    "malicious-ip-address",
                    ThreatSeverity.HIGH,
                    "THREAT_FEED",
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now()
                ),
                new ThreatIndicator(
                    "MALWARE_SIGNATURE",
                    "known-malware-hash",
                    ThreatSeverity.CRITICAL,
                    "AV_SCANNER",
                    LocalDateTime.now().minusMinutes(30),
                    LocalDateTime.now()
                )
            );
            
            var request = new ThreatAssessmentRequest(
                "source-malicious",
                "critical-resource",
                ThreatType.MALWARE,
                maliciousIndicators,
                Map.of("severity", "HIGH"),
                LocalDateTime.now()
            );
            
            // When
            var result = zeroTrustSecurityUseCase.assessThreat(request).join();
            
            // Then
            assertThat(result.threatLevel()).isIn(ThreatLevel.HIGH, ThreatLevel.CRITICAL);
            assertThat(result.identifiedThreats()).hasSize(maliciousIndicators.size());
            assertThat(result.recommendedMitigations()).isNotEmpty();
            assertThat(result.riskScore()).isGreaterThan(java.math.BigDecimal.valueOf(0.6));
        }
    }
    
    @Nested
    @DisplayName("Policy Enforcement")
    class PolicyEnforcementTests {
        
        @Test
        @DisplayName("Should enforce security policies successfully")
        void shouldEnforceSecurityPoliciesSuccessfully() {
            // Given
            var policies = List.of(
                new SecurityPolicy(
                    "policy-001",
                    "Standard Access Policy",
                    "Standard security policy for normal operations",
                    List.of("REQUIRE_AUTH", "VALIDATE_SESSION"),
                    true,
                    LocalDateTime.now().minusDays(1)
                ),
                new SecurityPolicy(
                    "policy-002",
                    "High-Risk Transaction Policy",
                    "Enhanced security for high-risk transactions",
                    List.of("REQUIRE_MFA", "MANAGER_APPROVAL"),
                    true,
                    LocalDateTime.now().minusDays(1)
                )
            );
            
            var request = new PolicyEnforcementRequest(
                "user-policy-test",
                "resource-policy-test",
                SecurityOperation.TRANSACTION,
                Map.of("transactionAmount", 5000),
                policies,
                EnforcementMode.BLOCK
            );
            
            // When
            var result = zeroTrustSecurityUseCase.enforceSecurityPolicies(request).join();
            
            // Then
            assertThat(result.enforcementSuccessful()).isTrue();
            assertThat(result.actionsApplied()).hasSize(policies.size());
            assertThat(result.violations()).isEmpty();
            assertThat(result.effectiveness().effectivenessScore())
                .isGreaterThan(java.math.BigDecimal.valueOf(0.8));
        }
        
        @Test
        @DisplayName("Should handle policy violations appropriately")
        void shouldHandlePolicyViolationsAppropriately() {
            // Given
            var policies = List.of(
                new SecurityPolicy(
                    "policy-strict",
                    "Strict Admin Policy",
                    "Strict policy for administrative actions",
                    List.of("REQUIRE_ADMIN_AUTH", "REQUIRE_IP_WHITELIST"),
                    true,
                    LocalDateTime.now().minusDays(1)
                )
            );
            
            var request = new PolicyEnforcementRequest(
                "user-non-admin",
                "admin-resource",
                SecurityOperation.ADMINISTRATIVE_ACTION,
                Map.of("userRole", "STANDARD"),
                policies,
                EnforcementMode.BLOCK
            );
            
            // When
            var result = zeroTrustSecurityUseCase.enforceSecurityPolicies(request).join();
            
            // Then
            assertThat(result.enforcementSuccessful()).isFalse();
            assertThat(result.violations()).isNotEmpty();
            assertThat(result.effectiveness().effectivenessScore())
                .isLessThan(java.math.BigDecimal.ONE);
        }
    }
    
    @Nested
    @DisplayName("FAPI2 Compliance")
    class FAPI2ComplianceTests {
        
        @Test
        @DisplayName("Should validate FAPI2 compliance for compliant client")
        void shouldValidateFAPI2ComplianceForCompliantClient() throws Exception {
            // Given
            var request = new ZeroTrustSecurityController.FAPI2SecurityRequestDto(
                "client-compliant-123",
                "openid payment",
                LocalDateTime.now()
            );
            
            // When & Then
            mockMvc.perform(post("/api/v1/security/zero-trust/fapi2-compliance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompliant").value(true))
                .andExpect(jsonPath("$.complianceChecks").exists());
        }
        
        @Test
        @DisplayName("Should detect FAPI2 non-compliance")
        void shouldDetectFAPI2NonCompliance() {
            // Given
            var request = new FAPI2SecurityRequest(
                null, // Missing client ID
                "invalid-scope",
                LocalDateTime.now()
            );
            
            // When
            var result = zeroTrustSecurityUseCase.validateFAPI2Compliance(request).join();
            
            // Then
            assertThat(result.isCompliant()).isFalse();
            assertThat(result.complianceChecks()).containsKey("clientAuthentication");
        }
    }
    
    @Nested
    @DisplayName("Performance Benchmarks")
    class PerformanceBenchmarkTests {
        
        @Test
        @DisplayName("Should validate security within performance thresholds")
        void shouldValidateSecurityWithinPerformanceThresholds() {
            // Given
            var requestCount = 500;
            var maxProcessingTime = 10000; // 10 seconds
            var requests = java.util.stream.IntStream.range(0, requestCount)
                .mapToObj(i -> new ZeroTrustSecurityRequest(
                    "perf-session-" + i,
                    "perf-user-" + i,
                    "perf-device-" + i,
                    "192.168.1." + (i % 255),
                    "PerfTestAgent/1.0",
                    LocalDateTime.now(),
                    Map.of("perfTest", true, "requestId", i),
                    SecurityOperation.TRANSACTION,
                    "perf-resource-" + i
                ))
                .toList();
            
            // When
            var startTime = System.currentTimeMillis();
            
            var futures = requests.stream()
                .map(zeroTrustSecurityUseCase::validateSecurity)
                .toList();
            
            var results = futures.stream()
                .map(future -> {
                    try {
                        return future.get(30, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
            
            var endTime = System.currentTimeMillis();
            var totalTime = endTime - startTime;
            var throughput = (requestCount * 1000.0) / totalTime;
            
            // Then
            assertThat(results).hasSize(requestCount);
            assertThat(totalTime).isLessThan(maxProcessingTime);
            assertThat(throughput).isGreaterThan(30.0); // > 30 validations/second
            
            // Performance metrics
            System.out.println("\n=== Zero Trust Security Performance Benchmark ===");
            System.out.printf("Requests: %d%n", requestCount);
            System.out.printf("Total Time: %d ms%n", totalTime);
            System.out.printf("Throughput: %.1f validations/second%n", throughput);
            System.out.printf("Average Latency: %.1f ms%n", (double) totalTime / requestCount);
            
            var successfulValidations = results.stream()
                .mapToInt(r -> r.isValid() ? 1 : 0)
                .sum();
            var successRate = (double) successfulValidations / requestCount;
            System.out.printf("Success Rate: %.1f%%%n", successRate * 100);
            
            // Performance assertions
            assertThat(successRate).isGreaterThan(0.8); // > 80% success rate
        }
        
        @Test
        @DisplayName("Should demonstrate Virtual Threads scalability")
        void shouldDemonstrateVirtualThreadsScalability() {
            // Given
            var concurrentSessions = 1000;
            var startTime = System.currentTimeMillis();
            
            // When - Create many concurrent verification requests
            var futures = java.util.stream.IntStream.range(0, concurrentSessions)
                .mapToObj(i -> {
                    var request = new ContinuousVerificationRequest(
                        "vt-session-" + i,
                        "vt-user-" + i,
                        LocalDateTime.now().minusMinutes(15),
                        List.of(new SecurityEvent(
                            "TEST_EVENT",
                            "Virtual thread test event",
                            LocalDateTime.now(),
                            Map.of("threadTest", true)
                        )),
                        Map.of("virtualThreadTest", true),
                        VerificationTrigger.TIME_BASED
                    );
                    return zeroTrustSecurityUseCase.performContinuousVerification(request);
                })
                .toList();
            
            // Wait for all to complete
            var results = futures.stream()
                .map(future -> {
                    try {
                        return future.get(20, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
            
            var endTime = System.currentTimeMillis();
            var totalTime = endTime - startTime;
            var throughput = (concurrentSessions * 1000.0) / totalTime;
            
            // Then
            assertThat(results).hasSize(concurrentSessions);
            assertThat(totalTime).isLessThan(15000); // Should complete within 15 seconds
            assertThat(throughput).isGreaterThan(50.0); // > 50 verifications/second
            
            System.out.println("\n=== Virtual Threads Scalability Test ===");
            System.out.printf("Concurrent Sessions: %d%n", concurrentSessions);
            System.out.printf("Total Time: %d ms%n", totalTime);
            System.out.printf("Throughput: %.1f verifications/second%n", throughput);
            System.out.printf("Average Response Time: %.1f ms%n", (double) totalTime / concurrentSessions);
        }
    }
    
    @Nested
    @DisplayName("Error Handling and Resilience")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle invalid requests gracefully")
        void shouldHandleInvalidRequestsGracefully() throws Exception {
            // Given
            var invalidRequest = "{ \"invalid\": \"json\" }";
            
            // When & Then
            mockMvc.perform(post("/api/v1/security/zero-trust/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
        
        @Test
        @DisplayName("Should handle service failures with appropriate fallbacks")
        void shouldHandleServiceFailuresWithAppropriateFallbacks() {
            // Given
            var request = new ZeroTrustSecurityRequest(
                "error-session",
                "error-user",
                "error-device",
                "192.168.1.1",
                "ErrorTestAgent/1.0",
                LocalDateTime.now(),
                Map.of("simulateError", true),
                SecurityOperation.TRANSACTION,
                "error-resource"
            );
            
            // When
            var result = zeroTrustSecurityUseCase.validateSecurity(request).join();
            
            // Then - Should provide a result even if some validations fail
            assertThat(result).isNotNull();
            assertThat(result.sessionId()).isEqualTo("error-session");
            assertThat(result.validationTime()).isNotNull();
        }
    }
    
    @Test
    @DisplayName("Should provide security status endpoint")
    void shouldProvideSecurityStatusEndpoint() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/security/zero-trust/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service").value("Zero Trust Security"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.version").exists());
    }
}