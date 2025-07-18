package com.bank.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test Suite for Threat Detection Filter
 * 
 * Following Red-Green-Refactor cycle:
 * 1. RED: Write failing tests first
 * 2. GREEN: Write minimal code to pass
 * 3. REFACTOR: Improve implementation
 * 
 * Security Test Coverage:
 * - SQL injection detection
 * - XSS prevention
 * - Path traversal detection
 * - Command injection prevention
 * - Rate limiting
 * - IP blocking
 * - Behavioral analysis
 * - Security guardrails
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Threat Detection Filter - TDD Security Tests")
class ThreatDetectionFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private PrintWriter writer;
    
    private ThreatDetectionFilterTDD threatDetectionFilter;
    
    @BeforeEach
    void setUp() throws Exception {
        // GREEN phase - using TDD implementation
        threatDetectionFilter = new ThreatDetectionFilterTDD();
        
        // Setup common mocks
        when(response.getWriter()).thenReturn(writer);
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getRequestURI()).thenReturn("/api/v1/customers");
        when(request.getMethod()).thenReturn("GET");
    }
    
    @Nested
    @DisplayName("RED Phase - SQL Injection Detection")
    class SQLInjectionDetectionTests {
        
        @Test
        @DisplayName("Should detect SQL injection in query parameters")
        void shouldDetectSQLInjectionInQueryParameters() throws Exception {
            // Arrange
            String maliciousQuery = "id=1' OR '1'='1'; DROP TABLE users; --";
            when(request.getQueryString()).thenReturn(maliciousQuery);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected and request was blocked
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
            verify(filterChain, never()).doFilter(request, response);
        }
        
        @Test
        @DisplayName("Should detect SQL injection in headers")
        void shouldDetectSQLInjectionInHeaders() throws Exception {
            // Arrange
            String maliciousHeader = "admin' UNION SELECT * FROM users WHERE '1'='1";
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Vector.of("X-Custom-Header")));
            when(request.getHeader("X-Custom-Header")).thenReturn(maliciousHeader);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        }
        
        @Test
        @DisplayName("Should detect advanced SQL injection patterns")
        void shouldDetectAdvancedSQLInjectionPatterns() throws Exception {
            // Arrange
            String[] maliciousQueries = {
                "id=1; INSERT INTO users VALUES('hacker', 'password')",
                "search=%27%20OR%20%271%27%3D%271", // URL encoded ' OR '1'='1
                "filter=1' AND (SELECT COUNT(*) FROM users) > 0 --"
            };
            
            for (String query : maliciousQueries) {
                when(request.getQueryString()).thenReturn(query);
                
                // Act & Assert - This should FAIL initially
                threatDetectionFilter.doFilter(request, response, filterChain);
                
                // Verify threat was detected
                verify(response, atLeastOnce()).setStatus(HttpStatus.FORBIDDEN.value());
                
                // Reset mock
                reset(response);
                when(response.getWriter()).thenReturn(writer);
            }
        }
    }
    
    @Nested
    @DisplayName("RED Phase - XSS Prevention")
    class XSSPreventionTests {
        
        @Test
        @DisplayName("Should detect XSS in query parameters")
        void shouldDetectXSSInQueryParameters() throws Exception {
            // Arrange
            String maliciousQuery = "search=<script>alert('XSS')</script>";
            when(request.getQueryString()).thenReturn(maliciousQuery);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        }
        
        @Test
        @DisplayName("Should detect XSS in referer header")
        void shouldDetectXSSInRefererHeader() throws Exception {
            // Arrange
            String maliciousReferer = "https://evil.com/<iframe src=javascript:alert('XSS')></iframe>";
            when(request.getHeader("Referer")).thenReturn(maliciousReferer);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        }
        
        @Test
        @DisplayName("Should detect various XSS attack vectors")
        void shouldDetectVariousXSSAttackVectors() throws Exception {
            // Arrange
            String[] xssVectors = {
                "javascript:alert('XSS')",
                "onload=alert('XSS')",
                "onerror=alert('XSS')",
                "<img src=x onerror=alert('XSS')>",
                "vbscript:msgbox('XSS')"
            };
            
            for (String xssVector : xssVectors) {
                when(request.getQueryString()).thenReturn("param=" + xssVector);
                
                // Act & Assert - This should FAIL initially
                threatDetectionFilter.doFilter(request, response, filterChain);
                
                // Verify threat was detected
                verify(response, atLeastOnce()).setStatus(HttpStatus.FORBIDDEN.value());
                
                // Reset mock
                reset(response);
                when(response.getWriter()).thenReturn(writer);
            }
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Path Traversal Detection")
    class PathTraversalDetectionTests {
        
        @Test
        @DisplayName("Should detect path traversal in URI")
        void shouldDetectPathTraversalInURI() throws Exception {
            // Arrange
            String maliciousURI = "/api/v1/files/../../../etc/passwd";
            when(request.getRequestURI()).thenReturn(maliciousURI);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        }
        
        @Test
        @DisplayName("Should detect URL encoded path traversal")
        void shouldDetectURLEncodedPathTraversal() throws Exception {
            // Arrange
            String maliciousURI = "/api/v1/files/%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd";
            when(request.getRequestURI()).thenReturn(maliciousURI);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        }
        
        @Test
        @DisplayName("Should detect Windows path traversal")
        void shouldDetectWindowsPathTraversal() throws Exception {
            // Arrange
            String maliciousURI = "/api/v1/files/..\\..\\..\\windows\\system32\\config\\sam";
            when(request.getRequestURI()).thenReturn(maliciousURI);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Command Injection Prevention")
    class CommandInjectionPreventionTests {
        
        @Test
        @DisplayName("Should detect command injection in query parameters")
        void shouldDetectCommandInjectionInQueryParameters() throws Exception {
            // Arrange
            String maliciousQuery = "file=test.txt; cat /etc/passwd";
            when(request.getQueryString()).thenReturn(maliciousQuery);
            
            // Act & Assert - This should FAIL initially
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Verify threat was detected
            verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        }
        
        @Test
        @DisplayName("Should detect various command injection patterns")
        void shouldDetectVariousCommandInjectionPatterns() throws Exception {
            // Arrange
            String[] commandInjections = {
                "file=test.txt | whoami",
                "param=value && ls -la",
                "input=data; rm -rf /",
                "field=`cat /etc/passwd`",
                "value=$(netstat -an)"
            };
            
            for (String injection : commandInjections) {
                when(request.getQueryString()).thenReturn(injection);
                
                // Act & Assert - This should FAIL initially
                threatDetectionFilter.doFilter(request, response, filterChain);
                
                // Verify threat was detected
                verify(response, atLeastOnce()).setStatus(HttpStatus.FORBIDDEN.value());
                
                // Reset mock
                reset(response);
                when(response.getWriter()).thenReturn(writer);
            }
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Rate Limiting")
    class RateLimitingTests {
        
        @Test
        @DisplayName("Should block requests exceeding rate limit")
        void shouldBlockRequestsExceedingRateLimit() throws Exception {
            // Arrange
            String clientIP = "192.168.1.100";
            when(request.getRemoteAddr()).thenReturn(clientIP);
            
            // Act - Send multiple requests rapidly
            for (int i = 0; i < 110; i++) { // Exceed limit of 100 requests per minute
                threatDetectionFilter.doFilter(request, response, filterChain);
            }
            
            // Assert - Last requests should be blocked
            verify(response, atLeastOnce()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }
        
        @Test
        @DisplayName("Should handle rate limiting per IP address")
        void shouldHandleRateLimitingPerIPAddress() throws Exception {
            // Arrange
            String clientIP1 = "192.168.1.100";
            String clientIP2 = "192.168.1.101";
            
            // Act - Send requests from different IPs
            when(request.getRemoteAddr()).thenReturn(clientIP1);
            for (int i = 0; i < 50; i++) {
                threatDetectionFilter.doFilter(request, response, filterChain);
            }
            
            when(request.getRemoteAddr()).thenReturn(clientIP2);
            for (int i = 0; i < 50; i++) {
                threatDetectionFilter.doFilter(request, response, filterChain);
            }
            
            // Assert - Both IPs should be within limits
            verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Suspicious User Agent Detection")
    class SuspiciousUserAgentTests {
        
        @Test
        @DisplayName("Should detect malicious user agents")
        void shouldDetectMaliciousUserAgents() throws Exception {
            // Arrange
            String[] maliciousUserAgents = {
                "sqlmap/1.0",
                "Nikto/2.1.6",
                "Mozilla/5.0 (compatible; Nmap Scripting Engine)",
                "OWASP ZAP",
                "Metasploit"
            };
            
            for (String userAgent : maliciousUserAgents) {
                when(request.getHeader("User-Agent")).thenReturn(userAgent);
                
                // Act & Assert - This should FAIL initially
                threatDetectionFilter.doFilter(request, response, filterChain);
                
                // Verify threat was detected
                verify(response, atLeastOnce()).setStatus(HttpStatus.FORBIDDEN.value());
                
                // Reset mock
                reset(response);
                when(response.getWriter()).thenReturn(writer);
            }
        }
    }
    
    @Nested
    @DisplayName("RED Phase - IP Blocking")
    class IPBlockingTests {
        
        @Test
        @DisplayName("Should automatically block IPs with high threat score")
        void shouldAutomaticallyBlockIPsWithHighThreatScore() throws Exception {
            // Arrange
            String maliciousIP = "192.168.1.200";
            when(request.getRemoteAddr()).thenReturn(maliciousIP);
            when(request.getQueryString()).thenReturn("id=1' OR '1'='1"); // SQL injection
            
            // Act - Send multiple malicious requests to trigger blocking
            for (int i = 0; i < 3; i++) {
                threatDetectionFilter.doFilter(request, response, filterChain);
                reset(response);
                when(response.getWriter()).thenReturn(writer);
            }
            
            // Assert - IP should be blocked
            threatDetectionFilter.doFilter(request, response, filterChain);
            verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        }
        
        @Test
        @DisplayName("Should unblock IPs after timeout period")
        void shouldUnblockIPsAfterTimeoutPeriod() throws Exception {
            // This test will verify that blocked IPs are automatically unblocked
            // after the configured timeout period
            // Implementation depends on the blocking strategy
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Security Boundary Tests")
    class SecurityBoundaryTests {
        
        @Test
        @DisplayName("Should handle extremely long requests")
        void shouldHandleExtremelyLongRequests() throws Exception {
            // Arrange
            String longQuery = "param=" + "a".repeat(100000); // 100KB query
            when(request.getQueryString()).thenReturn(longQuery);
            
            // Act & Assert - Should handle gracefully without crashing
            assertThatCode(() -> threatDetectionFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        }
        
        @Test
        @DisplayName("Should handle concurrent requests safely")
        void shouldHandleConcurrentRequestsSafely() throws Exception {
            // Test thread safety with concurrent requests
            // Will be implemented in GREEN phase
        }
        
        @Test
        @DisplayName("Should handle null and empty values gracefully")
        void shouldHandleNullAndEmptyValuesGracefully() throws Exception {
            // Arrange
            when(request.getQueryString()).thenReturn(null);
            when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
            
            // Act & Assert - Should not crash
            assertThatCode(() -> threatDetectionFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Should process requests within time limit")
        void shouldProcessRequestsWithinTimeLimit() throws Exception {
            // Arrange
            long startTime = System.currentTimeMillis();
            
            // Act
            threatDetectionFilter.doFilter(request, response, filterChain);
            
            // Assert - Should complete within 50ms
            long endTime = System.currentTimeMillis();
            assertThat(endTime - startTime).isLessThan(50L);
        }
        
        @Test
        @DisplayName("Should handle high request volume")
        void shouldHandleHighRequestVolume() throws Exception {
            // Test processing of many requests without performance degradation
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 1000; i++) {
                threatDetectionFilter.doFilter(request, response, filterChain);
            }
            
            long endTime = System.currentTimeMillis();
            // Should process 1000 requests in under 1 second
            assertThat(endTime - startTime).isLessThan(1000L);
        }
    }
    
    @Nested
    @DisplayName("RED Phase - Error Handling")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle filter chain exceptions gracefully")
        void shouldHandleFilterChainExceptionsGracefully() throws Exception {
            // Arrange
            doThrow(new ServletException("Filter chain error")).when(filterChain).doFilter(request, response);
            
            // Act & Assert - Should propagate exception but not crash
            assertThatThrownBy(() -> threatDetectionFilter.doFilter(request, response, filterChain))
                .isInstanceOf(ServletException.class);
        }
        
        @Test
        @DisplayName("Should handle response writing errors")
        void shouldHandleResponseWritingErrors() throws Exception {
            // Arrange
            when(response.getWriter()).thenThrow(new IOException("Response writing failed"));
            when(request.getQueryString()).thenReturn("id=1' OR '1'='1"); // SQL injection
            
            // Act & Assert - Should handle gracefully
            assertThatCode(() -> threatDetectionFilter.doFilter(request, response, filterChain))
                .doesNotThrowAnyException();
        }
    }
}