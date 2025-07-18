package com.bank.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * TDD Implementation of Threat Detection Filter
 * 
 * GREEN Phase: Minimal implementation to pass all tests
 * 
 * Implements comprehensive threat detection and prevention:
 * - SQL injection detection
 * - XSS (Cross-Site Scripting) prevention
 * - Path traversal attack detection
 * - Command injection prevention
 * - Suspicious pattern detection
 * - Rate limiting per IP
 * - Behavioral analysis
 * - Automatic IP blocking
 */
@Component
public class ThreatDetectionFilterTDD extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreatDetectionFilterTDD.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY");
    private static final Logger threatLogger = LoggerFactory.getLogger("THREAT");
    
    private final ObjectMapper objectMapper;
    
    // Threat detection patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(('|(\\-\\-)|(;)|(\\|)|(\\*)|(%27)|(%2D%2D)|(%7C)|(%2A))" +
        ".*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror)|" +
        "(union|select|insert|update|delete|drop|create|alter|exec|execute).*)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(<script[^>]*>.*</script>|<iframe[^>]*>.*</iframe>|javascript:|vbscript:|onload=|onerror=|onmouseover=|onfocus=|onblur=|onchange=|onclick=|<img[^>]*onerror)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(?i).*(\\.\\.[\\/\\\\]|\\.\\.%2f|\\.\\.%5c|%2e%2e[\\/\\\\]|%2e%2e%2f|%2e%2e%5c)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(\\||&|;|\\$|`|\\(|\\)|\\{|\\}|\\[|\\]|>|<|cat|ls|pwd|whoami|id|ps|netstat|ifconfig|ping|nslookup|wget|curl|rm)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern SUSPICIOUS_USER_AGENT_PATTERN = Pattern.compile(
        "(?i).*(sqlmap|nmap|nikto|burp|owasp|zap|w3af|havij|pangolin|metasploit|nessus|openvas|acunetix|netsparker|qualys|rapid7)",
        Pattern.CASE_INSENSITIVE
    );
    
    // IP tracking for behavioral analysis
    private final Map<String, IPThreatInfo> ipThreatMap = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockedIPs = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int THREAT_SCORE_THRESHOLD = 10;
    private static final int AUTO_BLOCK_DURATION_MINUTES = 30;
    
    public ThreatDetectionFilterTDD() {
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String clientIP = getClientIpAddress(request);
        
        try {
            // Check if IP is blocked
            if (isIPBlocked(clientIP)) {
                handleBlockedIP(request, response, clientIP);
                return;
            }
            
            // Perform threat detection
            ThreatDetectionResult result = analyzeThreat(request, clientIP);
            
            // Update IP threat information
            updateIPThreatInfo(clientIP, result);
            
            // Check if request should be blocked
            if (result.isBlocked()) {
                handleThreatDetected(request, response, result);
                return;
            }
            
            // Log suspicious activity
            if (result.isSuspicious()) {
                logSuspiciousActivity(request, clientIP, result);
            }
            
            // Continue with request processing
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error in threat detection filter", e);
            // Continue processing on error to avoid disrupting legitimate requests
            filterChain.doFilter(request, response);
        }
    }
    
    private ThreatDetectionResult analyzeThreat(HttpServletRequest request, String clientIP) {
        ThreatDetectionResult result = new ThreatDetectionResult();
        
        // Check rate limiting
        if (isRateLimitExceeded(clientIP)) {
            result.addThreat(ThreatType.RATE_LIMIT_EXCEEDED, "Too many requests from IP", 5);
        }
        
        // Check user agent
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && SUSPICIOUS_USER_AGENT_PATTERN.matcher(userAgent).matches()) {
            result.addThreat(ThreatType.SUSPICIOUS_USER_AGENT, "Suspicious user agent detected", 8);
        }
        
        // Check for SQL injection
        if (containsSQLInjection(request)) {
            result.addThreat(ThreatType.SQL_INJECTION, "SQL injection attempt detected", 10);
        }
        
        // Check for XSS
        if (containsXSS(request)) {
            result.addThreat(ThreatType.XSS, "XSS attempt detected", 8);
        }
        
        // Check for path traversal
        if (containsPathTraversal(request)) {
            result.addThreat(ThreatType.PATH_TRAVERSAL, "Path traversal attempt detected", 9);
        }
        
        // Check for command injection
        if (containsCommandInjection(request)) {
            result.addThreat(ThreatType.COMMAND_INJECTION, "Command injection attempt detected", 10);
        }
        
        return result;
    }
    
    private boolean containsSQLInjection(HttpServletRequest request) {
        // Check URL parameters
        String queryString = request.getQueryString();
        if (queryString != null && SQL_INJECTION_PATTERN.matcher(queryString).matches()) {
            return true;
        }
        
        // Check headers
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                if (headerValue != null && SQL_INJECTION_PATTERN.matcher(headerValue).matches()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean containsXSS(HttpServletRequest request) {
        // Check URL parameters
        String queryString = request.getQueryString();
        if (queryString != null && XSS_PATTERN.matcher(queryString).matches()) {
            return true;
        }
        
        // Check headers
        String referer = request.getHeader("Referer");
        if (referer != null && XSS_PATTERN.matcher(referer).matches()) {
            return true;
        }
        
        return false;
    }
    
    private boolean containsPathTraversal(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && PATH_TRAVERSAL_PATTERN.matcher(uri).matches();
    }
    
    private boolean containsCommandInjection(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString != null && COMMAND_INJECTION_PATTERN.matcher(queryString).matches()) {
            return true;
        }
        
        return false;
    }
    
    private boolean isRateLimitExceeded(String clientIP) {
        IPThreatInfo ipInfo = ipThreatMap.computeIfAbsent(clientIP, k -> new IPThreatInfo());
        
        // Clean up old requests
        ipInfo.cleanupOldRequests();
        
        // Increment request count
        ipInfo.incrementRequestCount();
        
        return ipInfo.getRequestCount() > MAX_REQUESTS_PER_MINUTE;
    }
    
    private void updateIPThreatInfo(String clientIP, ThreatDetectionResult result) {
        IPThreatInfo ipInfo = ipThreatMap.computeIfAbsent(clientIP, k -> new IPThreatInfo());
        
        // Update threat score
        ipInfo.addThreatScore(result.getTotalScore());
        
        // Check if IP should be blocked
        if (ipInfo.getThreatScore() >= THREAT_SCORE_THRESHOLD) {
            blockIP(clientIP);
        }
    }
    
    private void blockIP(String clientIP) {
        Instant blockUntil = Instant.now().plus(AUTO_BLOCK_DURATION_MINUTES, ChronoUnit.MINUTES);
        blockedIPs.put(clientIP, blockUntil);
        
        threatLogger.warn("IP {} automatically blocked due to high threat score", clientIP);
    }
    
    private boolean isIPBlocked(String clientIP) {
        Instant blockUntil = blockedIPs.get(clientIP);
        if (blockUntil != null) {
            if (Instant.now().isAfter(blockUntil)) {
                // Remove expired block
                blockedIPs.remove(clientIP);
                return false;
            }
            return true;
        }
        return false;
    }
    
    private void handleBlockedIP(HttpServletRequest request, HttpServletResponse response, 
                                String clientIP) throws IOException {
        
        threatLogger.warn("Blocked IP {} attempted to access {}", clientIP, request.getRequestURI());
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/problem+json");
        
        try {
            Map<String, Object> errorResponse = Map.of(
                "type", "https://banking.example.com/problems/ip-blocked",
                "title", "IP Address Blocked",
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "detail", "Your IP address has been temporarily blocked due to suspicious activity",
                "instance", request.getRequestURI(),
                "timestamp", Instant.now().toString()
            );
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } catch (Exception e) {
            logger.warn("Failed to write error response", e);
        }
    }
    
    private void handleThreatDetected(HttpServletRequest request, HttpServletResponse response, 
                                     ThreatDetectionResult result) throws IOException {
        
        String clientIP = getClientIpAddress(request);
        
        // Log threat
        threatLogger.warn("Threat detected from IP {}: {} (Score: {})", 
            clientIP, result.getThreats(), result.getTotalScore());
        
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/problem+json");
        
        try {
            Map<String, Object> errorResponse = Map.of(
                "type", "https://banking.example.com/problems/threat-detected",
                "title", "Security Threat Detected",
                "status", HttpStatus.FORBIDDEN.value(),
                "detail", "Request blocked due to security threat detection",
                "instance", request.getRequestURI(),
                "timestamp", Instant.now().toString()
            );
            
            objectMapper.writeValue(response.getWriter(), errorResponse);
        } catch (Exception e) {
            logger.warn("Failed to write error response", e);
        }
    }
    
    private void logSuspiciousActivity(HttpServletRequest request, String clientIP, 
                                      ThreatDetectionResult result) {
        securityLogger.warn("Suspicious activity from IP {}: {} | URI: {} | Score: {}",
            clientIP, result.getThreats(), request.getRequestURI(), result.getTotalScore());
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For", "X-Real-IP", "X-Originating-IP", "CF-Connecting-IP", "True-Client-IP"
        };
        
        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
    
    // Inner classes
    
    private static class IPThreatInfo {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final AtomicLong threatScore = new AtomicLong(0);
        private final Map<String, Instant> requests = new ConcurrentHashMap<>();
        
        public void incrementRequestCount() {
            requestCount.incrementAndGet();
            requests.put(String.valueOf(System.nanoTime()), Instant.now());
        }
        
        public void addThreatScore(int score) {
            threatScore.addAndGet(score);
        }
        
        public void cleanupOldRequests() {
            Instant cutoff = Instant.now().minus(1, ChronoUnit.MINUTES);
            requests.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
            requestCount.set(requests.size());
        }
        
        public int getRequestCount() {
            return requestCount.get();
        }
        
        public long getThreatScore() {
            return threatScore.get();
        }
    }
    
    private static class ThreatDetectionResult {
        private final Map<ThreatType, String> threats = new ConcurrentHashMap<>();
        private final Map<ThreatType, Integer> scores = new ConcurrentHashMap<>();
        
        public void addThreat(ThreatType type, String description, int score) {
            threats.put(type, description);
            scores.put(type, score);
        }
        
        public boolean isBlocked() {
            return getTotalScore() >= THREAT_SCORE_THRESHOLD;
        }
        
        public boolean isSuspicious() {
            return getTotalScore() >= 5;
        }
        
        public int getTotalScore() {
            return scores.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public Map<ThreatType, String> getThreats() {
            return threats;
        }
    }
    
    private enum ThreatType {
        SQL_INJECTION,
        XSS,
        PATH_TRAVERSAL,
        COMMAND_INJECTION,
        SUSPICIOUS_USER_AGENT,
        RATE_LIMIT_EXCEEDED
    }
}