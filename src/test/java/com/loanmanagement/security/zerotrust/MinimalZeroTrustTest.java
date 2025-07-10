package com.loanmanagement.security.zerotrust;

import com.loanmanagement.security.zerotrust.domain.model.*;
import com.loanmanagement.security.zerotrust.domain.service.ZeroTrustSecurityDomainService;
import com.loanmanagement.security.zerotrust.application.service.ZeroTrustSecurityApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Minimal test to verify Zero Trust Security hexagonal architecture
 * 
 * ✅ Clean architecture: Domain → Application → Infrastructure
 * ✅ Virtual Threads: Concurrent processing capability  
 * ✅ Ports/Adapters: Proper dependency inversion
 */
@DisplayName("Minimal Zero Trust Security Test")
class MinimalZeroTrustTest {
    
    @Test
    @DisplayName("Should validate security using clean hexagonal architecture")
    void shouldValidateSecurityUsingCleanArchitecture() {
        // Given - Clean domain components without external dependencies
        var domainService = new ZeroTrustSecurityDomainService();
        var applicationService = new ZeroTrustSecurityApplicationService(domainService);
        
        var request = new ZeroTrustSecurityRequest(
            "test-session-123",
            "test-user-456", 
            "test-device-789",
            "192.168.1.100",
            "TestAgent/1.0",
            LocalDateTime.now(),
            Map.of("testContext", "architecture-validation"),
            SecurityOperation.TRANSACTION,
            "test-resource"
        );
        
        // When - Process through application service (hexagonal architecture)
        var result = applicationService.validateSecurity(request).join();
        
        // Then - Verify clean domain response
        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isEqualTo("test-session-123");
        assertThat(result.isValid()).isTrue();
        assertThat(result.securityLevel()).isNotNull();
        assertThat(result.validationTime()).isNotNull();
        assertThat(result.securityMetrics()).containsKey("operation");
        
        System.out.println("✅ Zero Trust Security - Hexagonal Architecture: WORKING");
    }
    
    @Test
    @DisplayName("Should process concurrent validations with Virtual Threads")
    void shouldProcessConcurrentValidationsWithVirtualThreads() {
        // Given
        var domainService = new ZeroTrustSecurityDomainService();
        var applicationService = new ZeroTrustSecurityApplicationService(domainService);
        var requestCount = 20;
        var startTime = System.currentTimeMillis();
        
        // When - Process concurrent requests using Virtual Threads
        var futures = java.util.stream.IntStream.range(0, requestCount)
            .mapToObj(i -> {
                var request = new ZeroTrustSecurityRequest(
                    "concurrent-session-" + i,
                    "concurrent-user-" + i,
                    "concurrent-device-" + i,
                    "192.168.1." + (i % 255),
                    "ConcurrentTestAgent/1.0",
                    LocalDateTime.now(),
                    Map.of("concurrentTest", true, "requestIndex", i),
                    SecurityOperation.API_CALL,
                    "concurrent-resource-" + i
                );
                return applicationService.validateSecurity(request);
            })
            .toList();
        
        var results = futures.stream()
            .map(future -> {
                try {
                    return future.get(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .toList();
        
        var endTime = System.currentTimeMillis();
        var totalTime = endTime - startTime;
        var throughput = (requestCount * 1000.0) / totalTime;
        
        // Then - Verify performance and correctness
        assertThat(results).hasSize(requestCount);
        assertThat(totalTime).isLessThan(2000); // Should complete within 2 seconds
        assertThat(throughput).isGreaterThan(10.0); // > 10 validations/second
        
        System.out.printf("✅ Virtual Threads Performance: %d concurrent validations in %d ms (%.1f/sec)%n", 
                         requestCount, totalTime, throughput);
    }
    
    @Test 
    @DisplayName("Should demonstrate clean domain logic separation")
    void shouldDemonstrateCleanDomainLogicSeparation() {
        // Given - Pure domain service without infrastructure
        var domainService = new ZeroTrustSecurityDomainService();
        
        var request = new ZeroTrustSecurityRequest(
            "domain-test-session",
            "domain-test-user",
            "domain-test-device", 
            "10.0.0.1",
            "DomainTestAgent/1.0",
            LocalDateTime.now(),
            Map.of("domainTest", true),
            SecurityOperation.DATA_ACCESS,
            "domain-test-resource"
        );
        
        // When - Call domain service directly
        var result = domainService.evaluateOverallSecurity(request);
        
        // Then - Verify domain-centric result
        assertThat(result).isNotNull();
        assertThat(result.isValid()).isTrue();
        assertThat(result.securityLevel()).isEqualTo(SecurityLevel.MEDIUM);
        assertThat(result.violations()).isEmpty();
        assertThat(result.recommendations()).isEmpty();
        
        System.out.println("✅ Domain Logic Separation: WORKING");
    }
}