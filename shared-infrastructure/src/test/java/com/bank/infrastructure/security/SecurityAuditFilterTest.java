package com.bank.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Security Audit Filter
 * 
 * Following Red-Green-Refactor cycle:
 * 1. RED: Write failing tests first
 * 2. GREEN: Write minimal code to pass
 * 3. REFACTOR: Improve implementation
 * 
 * Security Audit Test Coverage:
 * - Request access logging
 * - Authentication event tracking
 * - Authorization monitoring
 * - Sensitive data access logging
 * - Admin action tracking
 * - Compliance event logging
 * - Performance monitoring
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security Audit Filter - TDD Security Tests")
class SecurityAuditFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private HttpSession session;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private SecurityAuditFilter securityAuditFilter;
    
    @BeforeEach
    void setUp() {
        // This will fail initially - RED phase
        securityAuditFilter = new SecurityAuditFilter();
        
        // Setup common mocks
        when(request.getRequestURI()).thenReturn("/api/v1/customers");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Request-ID")).thenReturn("req-123");
        when(request.getHeader("X-Correlation-ID")).thenReturn("corr-456");
        when(response.getStatus()).thenReturn(200);
        
        // Setup Redis mocks
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // Setup security context
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("customer123");
        when(authentication.getAuthorities()).thenReturn(List.of(() -> "ROLE_CUSTOMER"));
    }
    
    @Nested
    @DisplayName("RED Phase - Request Access Logging")
    class RequestAccessLoggingTests {
        
        @Test
        @DisplayName("Should log successful request access")
        void shouldLogSuccessfulRequestAccess() throws Exception {
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify filter chain was called
            verify(filterChain).doFilter(request, response);
            
            // Verify audit event was stored in Redis
            verify(valueOperations).set(
                contains("audit:event:"),
                anyString(),
                eq(24L),
                eq(TimeUnit.HOURS)
            );
        }
        
        @Test
        @DisplayName("Should log failed request access")
        void shouldLogFailedRequestAccess() throws Exception {
            // Arrange
            ServletException exception = new ServletException("Access denied");
            doThrow(exception).when(filterChain).doFilter(request, response);
            
            // Act & Assert - This should FAIL initially
            assertThatThrownBy(() -> securityAuditFilter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class);
            
            // Verify failure event was logged
            verify(valueOperations).set(
                contains("audit:event:"),
                anyString(),
                eq(24L),
                eq(TimeUnit.HOURS)
            );
        }
        
        @Test
        @DisplayName("Should capture request metadata")
        void shouldCaptureRequestMetadata() throws Exception {
            // Arrange
            when(request.getSession(false)).thenReturn(session);
            when(session.getId()).thenReturn("session-789");
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify metadata was captured
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("\"requestId\":\"req-123\""),
                anyLong(),
                any(TimeUnit.class)
            );
            
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("\"correlationId\":\"corr-456\""),
                anyLong(),
                any(TimeUnit.class)
            );
        }
        
        @Test
        @DisplayName("Should handle anonymous users")
        void shouldHandleAnonymousUsers() throws Exception {
            // Arrange
            when(authentication.isAuthenticated()).thenReturn(false);
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify anonymous user was logged
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("anonymous"),
                anyLong(),
                any(TimeUnit.class)
            );
        }
        
        @Test
        @DisplayName("Should extract client IP from various headers")
        void shouldExtractClientIPFromVariousHeaders() throws Exception {
            // Arrange
            when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");
            when(request.getRemoteAddr()).thenReturn("10.0.0.1");
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify correct IP was extracted (first from X-Forwarded-For)
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("\"clientIp\":\"203.0.113.1\""),
                anyLong(),
                any(TimeUnit.class)
            );
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Sensitive Data Access Logging")
    class SensitiveDataAccessLoggingTests {
        
        @Test
        @DisplayName("Should log sensitive data access for customer endpoints")
        void shouldLogSensitiveDataAccessForCustomerEndpoints() throws Exception {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/v1/customers/123/profile");
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify sensitive data access was logged
            verify(valueOperations).set(
                contains("audit:event:sensitive:"),
                contains("CUSTOMER_DATA"),
                eq(7L),
                eq(TimeUnit.DAYS)
            );
        }
        
        @Test
        @DisplayName("Should log sensitive data access for loan endpoints")
        void shouldLogSensitiveDataAccessForLoanEndpoints() throws Exception {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/v1/loans/456/details");
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify loan data access was logged
            verify(valueOperations).set(
                contains("audit:event:sensitive:"),
                contains("LOAN_DATA"),
                eq(7L),
                eq(TimeUnit.DAYS)
            );
        }
        
        @Test
        @DisplayName("Should log sensitive data access for payment endpoints")
        void shouldLogSensitiveDataAccessForPaymentEndpoints() throws Exception {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/v1/payments/789/history");
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify payment data access was logged
            verify(valueOperations).set(
                contains("audit:event:sensitive:"),
                contains("PAYMENT_DATA"),
                eq(7L),
                eq(TimeUnit.DAYS)
            );
        }
        
        @Test
        @DisplayName("Should classify data types correctly")
        void shouldClassifyDataTypesCorrectly() throws Exception {
            // Test various endpoints and their data type classifications
            String[][] testCases = {
                {"/api/v1/customers", "CUSTOMER_DATA"},
                {"/api/v1/loans", "LOAN_DATA"},
                {"/api/v1/payments", "PAYMENT_DATA"},
                {"/api/v1/reports", "REPORT_DATA"}
            };
            
            for (String[] testCase : testCases) {
                String endpoint = testCase[0];
                String expectedDataType = testCase[1];
                
                when(request.getRequestURI()).thenReturn(endpoint);
                
                // Act & Assert - This should FAIL initially
                securityAuditFilter.doFilter(request, response, filterChain);
                
                // Verify correct data type was logged
                verify(valueOperations).set(
                    contains("audit:event:sensitive:"),
                    contains(expectedDataType),
                    anyLong(),
                    any(TimeUnit.class)
                );
                
                // Reset for next iteration
                reset(valueOperations);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            }
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Admin Action Logging")
    class AdminActionLoggingTests {
        
        @Test
        @DisplayName("Should log admin actions")
        void shouldLogAdminActions() throws Exception {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/v1/admin/users");
            when(authentication.getAuthorities()).thenReturn(List.of(() -> "ROLE_ADMIN"));
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify admin action was logged
            verify(valueOperations).set(
                contains("audit:event:admin:"),
                contains("USER_MANAGEMENT"),
                eq(30L),
                eq(TimeUnit.DAYS)
            );
        }
        
        @Test
        @DisplayName("Should classify admin actions correctly")
        void shouldClassifyAdminActionsCorrectly() throws Exception {
            // Test various admin endpoints
            String[][] testCases = {
                {"/api/v1/admin/users", "USER_MANAGEMENT"},
                {"/api/v1/admin/config", "CONFIGURATION"},
                {"/api/v1/admin/system", "SYSTEM_MANAGEMENT"}
            };
            
            for (String[] testCase : testCases) {
                String endpoint = testCase[0];
                String expectedActionType = testCase[1];
                
                when(request.getRequestURI()).thenReturn(endpoint);
                
                // Act & Assert - This should FAIL initially
                securityAuditFilter.doFilter(request, response, filterChain);
                
                // Verify correct action type was logged
                verify(valueOperations).set(
                    contains("audit:event:admin:"),
                    contains(expectedActionType),
                    anyLong(),
                    any(TimeUnit.class)
                );
                
                // Reset for next iteration
                reset(valueOperations);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            }
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Compliance Event Logging")
    class ComplianceEventLoggingTests {
        
        @Test
        @DisplayName("Should log compliance events")
        void shouldLogComplianceEvents() throws Exception {
            // Arrange
            when(request.getRequestURI()).thenReturn("/api/v1/customers/123/data");
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify compliance event was logged
            verify(valueOperations).set(
                contains("compliance:event:"),
                contains("DATA_PRIVACY"),
                eq(365L),
                eq(TimeUnit.DAYS)
            );
        }
        
        @Test
        @DisplayName("Should identify regulatory requirements")
        void shouldIdentifyRegulatoryRequirements() throws Exception {
            // Test various compliance scenarios
            String[][] testCases = {
                {"/api/v1/customers", "GDPR,PCI_DSS,KYC"},
                {"/api/v1/loans", "BASEL_III,IFRS_9,UAE_CENTRAL_BANK"},
                {"/api/v1/payments", "PCI_DSS,AML,UAE_CENTRAL_BANK"}
            };
            
            for (String[] testCase : testCases) {
                String endpoint = testCase[0];
                String expectedRequirements = testCase[1];
                
                when(request.getRequestURI()).thenReturn(endpoint);
                
                // Act & Assert - This should FAIL initially
                securityAuditFilter.doFilter(request, response, filterChain);
                
                // Verify correct requirements were identified
                verify(valueOperations).set(
                    contains("compliance:event:"),
                    contains(expectedRequirements),
                    anyLong(),
                    any(TimeUnit.class)
                );
                
                // Reset for next iteration
                reset(valueOperations);
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            }
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Authentication Method Detection")
    class AuthenticationMethodDetectionTests {
        
        @Test
        @DisplayName("Should detect JWT authentication")
        void shouldDetectJWTAuthentication() throws Exception {
            // Arrange
            Authentication jwtAuth = mock(Authentication.class);
            when(jwtAuth.getClass().getSimpleName()).thenReturn("JwtAuthenticationToken");
            when(jwtAuth.isAuthenticated()).thenReturn(true);
            when(jwtAuth.getName()).thenReturn("customer123");
            when(securityContext.getAuthentication()).thenReturn(jwtAuth);
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify JWT authentication was detected
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("\"authenticationMethod\":\"JWT\""),
                anyLong(),
                any(TimeUnit.class)
            );
        }
        
        @Test
        @DisplayName("Should detect OAuth2 authentication")
        void shouldDetectOAuth2Authentication() throws Exception {
            // Arrange
            Authentication oauth2Auth = mock(Authentication.class);
            when(oauth2Auth.getClass().getSimpleName()).thenReturn("OAuth2AuthenticationToken");
            when(oauth2Auth.isAuthenticated()).thenReturn(true);
            when(oauth2Auth.getName()).thenReturn("customer123");
            when(securityContext.getAuthentication()).thenReturn(oauth2Auth);
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify OAuth2 authentication was detected
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("\"authenticationMethod\":\"OAUTH2\""),
                anyLong(),
                any(TimeUnit.class)
            );
        }
        
        @Test
        @DisplayName("Should handle anonymous authentication")
        void shouldHandleAnonymousAuthentication() throws Exception {
            // Arrange
            when(securityContext.getAuthentication()).thenReturn(null);
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify anonymous authentication was detected
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("\"authenticationMethod\":\"ANONYMOUS\""),
                anyLong(),
                any(TimeUnit.class)
            );
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Performance Monitoring")
    class PerformanceMonitoringTests {
        
        @Test
        @DisplayName("Should measure response times")
        void shouldMeasureResponseTimes() throws Exception {
            // Arrange
            doAnswer(invocation -> {
                // Simulate processing time
                Thread.sleep(50);
                return null;
            }).when(filterChain).doFilter(request, response);
            
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify response time was captured
            verify(valueOperations).set(
                contains("audit:event:"),
                contains("\"responseTime\":"),
                anyLong(),
                any(TimeUnit.class)
            );
        }
        
        @Test
        @DisplayName("Should monitor filter performance")
        void shouldMonitorFilterPerformance() throws Exception {
            // Arrange
            long startTime = System.currentTimeMillis();
            
            // Act
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Assert - Filter should complete within reasonable time
            long endTime = System.currentTimeMillis();
            assertThat(endTime - startTime).isLessThan(100L);
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Error Handling")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle Redis failures gracefully")
        void shouldHandleRedisFailuresGracefully() throws Exception {
            // Arrange
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection failed"));
            
            // Act & Assert - Should not interrupt request processing
            assertThatCode(() -> securityAuditFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
            
            // Verify filter chain was still called
            verify(filterChain).doFilter(request, response);
        }
        
        @Test
        @DisplayName("Should handle serialization errors")
        void shouldHandleSerializationErrors() throws Exception {
            // Arrange
            when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Serialization failed"));
            
            // Act & Assert - Should handle gracefully
            assertThatCode(() -> securityAuditFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle missing headers gracefully")
        void shouldHandleMissingHeadersGracefully() throws Exception {
            // Arrange
            when(request.getHeader("X-Request-ID")).thenReturn(null);
            when(request.getHeader("X-Correlation-ID")).thenReturn(null);
            when(request.getHeader("User-Agent")).thenReturn(null);
            
            // Act & Assert - Should handle gracefully
            assertThatCode(() -> securityAuditFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Security Boundary Tests")
    class SecurityBoundaryTests {
        
        @Test
        @DisplayName("Should handle extremely long URIs")
        void shouldHandleExtremelyLongURIs() throws Exception {
            // Arrange
            String longURI = "/api/v1/customers/" + "a".repeat(10000);
            when(request.getRequestURI()).thenReturn(longURI);
            
            // Act & Assert - Should handle gracefully
            assertThatCode(() -> securityAuditFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle malicious headers")
        void shouldHandleMaliciousHeaders() throws Exception {
            // Arrange
            when(request.getHeader("X-Request-ID")).thenReturn("'; DROP TABLE audit_logs; --");
            when(request.getHeader("User-Agent")).thenReturn("<script>alert('XSS')</script>");
            
            // Act & Assert - Should handle safely
            assertThatCode(() -> securityAuditFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle concurrent requests safely")
        void shouldHandleConcurrentRequestsSafely() throws Exception {
            // Test thread safety with concurrent requests
            // Will be implemented in GREEN phase
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Audit Data Integrity")
    class AuditDataIntegrityTests {
        
        @Test
        @DisplayName("Should ensure audit events are immutable")
        void shouldEnsureAuditEventsAreImmutable() throws Exception {
            // Arrange
            String originalURI = "/api/v1/customers";
            when(request.getRequestURI()).thenReturn(originalURI);
            
            // Act
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Change request URI after processing
            when(request.getRequestURI()).thenReturn("/api/v1/loans");
            
            // Assert - Original audit event should remain unchanged
            verify(valueOperations).set(
                contains("audit:event:"),
                contains(originalURI),
                anyLong(),
                any(TimeUnit.class)
            );
        }
        
        @Test
        @DisplayName("Should validate audit event structure")
        void shouldValidateAuditEventStructure() throws Exception {
            // Act & Assert - This should FAIL initially
            securityAuditFilter.doFilter(request, response, filterChain);
            
            // Verify required fields are present
            verify(valueOperations).set(
                contains("audit:event:"),
                allOf(
                    contains("\"eventType\":"),
                    contains("\"timestamp\":"),
                    contains("\"requestId\":"),
                    contains("\"userId\":"),
                    contains("\"uri\":"),
                    contains("\"method\":"),
                    contains("\"clientIp\":")
                ),
                anyLong(),
                any(TimeUnit.class)
            );
        }
    }
}