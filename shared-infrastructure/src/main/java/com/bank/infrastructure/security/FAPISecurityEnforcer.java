package com.bank.infrastructure.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * FAPI 2.0 Security Enforcer
 * 
 * Implements Financial-grade API security requirements including:
 * - Client certificate validation (mTLS)
 * - FAPI-specific headers validation
 * - Request validation and enrichment
 * - Security event logging
 * - Attack detection and prevention
 */
@Component
public class FAPISecurityEnforcer extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(FAPISecurityEnforcer.class);
    
    // FAPI 2.0 required headers
    private static final String FAPI_FINANCIAL_ID = "X-FAPI-Financial-Id";
    private static final String FAPI_INTERACTION_ID = "X-FAPI-Interaction-Id";
    private static final String FAPI_CUSTOMER_IP = "X-FAPI-Customer-IP-Address";
    private static final String FAPI_AUTH_DATE = "X-FAPI-Auth-Date";
    
    // Security headers
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String CLIENT_ID = "X-Client-ID";
    
    // Pattern validation
    private static final Pattern FINANCIAL_ID_PATTERN = Pattern.compile("^[A-Z]{2}-[A-Z]{3}-[0-9]{6}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern IP_PATTERN = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Only apply FAPI enforcement to financial API endpoints
            if (!isFinancialAPIEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // 1. Validate mTLS certificate
            if (!validateMutualTLS(request)) {
                sendSecurityError(response, "FAPI_MTLS_REQUIRED", 
                    "Mutual TLS client certificate is required for FAPI endpoints", 
                    HttpStatus.UNAUTHORIZED);
                return;
            }
            
            // 2. Validate FAPI headers
            FAPIValidationResult fapiValidation = validateFAPIHeaders(request);
            if (!fapiValidation.isValid()) {
                sendSecurityError(response, "FAPI_HEADER_INVALID", 
                    fapiValidation.getErrorMessage(), 
                    HttpStatus.BAD_REQUEST);
                return;
            }
            
            // 3. Validate idempotency for state-changing operations
            if (isStateChangingOperation(request)) {
                if (!validateIdempotencyKey(request)) {
                    sendSecurityError(response, "IDEMPOTENCY_KEY_REQUIRED", 
                        "Idempotency-Key header is required for state-changing operations", 
                        HttpStatus.BAD_REQUEST);
                    return;
                }
            }
            
            // 4. Enrich request with security context
            enrichRequestWithSecurityContext(request, response);
            
            // 5. Apply additional FAPI security headers to response
            applyFAPISecurityHeaders(response);
            
            // 6. Log security event
            logSecurityEvent(request, "FAPI_REQUEST_VALIDATED");
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("FAPI security enforcement failed", e);
            sendSecurityError(response, "FAPI_SECURITY_ERROR", 
                "Internal security validation error", 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Check if the endpoint requires FAPI 2.0 compliance
     */
    private boolean isFinancialAPIEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/customers") ||
               path.startsWith("/api/v1/loans") ||
               path.startsWith("/api/v1/payments") ||
               path.startsWith("/api/v1/accounts");
    }
    
    /**
     * Validate Mutual TLS client certificate
     */
    private boolean validateMutualTLS(HttpServletRequest request) {
        // Check for client certificate in request attributes
        Object[] certs = (Object[]) request.getAttribute("javax.servlet.request.X509Certificate");
        X509Certificate clientCert = null;
        
        if (certs != null && certs.length > 0) {
            clientCert = (X509Certificate) certs[0];
        } else {
            // Check SSL_CLIENT_CERT header (from reverse proxy)
            String clientCertHeader = request.getHeader("SSL_CLIENT_CERT");
            if (clientCertHeader == null || clientCertHeader.trim().isEmpty()) {
                return false;
            }
        }
        
        try {
            // Validate certificate chain
            if (!validateCertificateChain(clientCert)) {
                logger.warn("Certificate chain validation failed for request: {}", request.getRequestURI());
                return false;
            }
            
            // Check certificate revocation status
            if (!checkCertificateRevocation(clientCert)) {
                logger.warn("Certificate revocation check failed for request: {}", request.getRequestURI());
                return false;
            }
            
            // Validate certificate expiration
            if (!isCertificateValid(clientCert)) {
                logger.warn("Certificate expired or not yet valid for request: {}", request.getRequestURI());
                return false;
            }
            
            // Extract and validate client ID from certificate
            String certClientId = extractClientIdFromCertificate(request);
            if (certClientId == null || !isValidClientId(certClientId)) {
                logger.warn("Invalid or missing client ID in certificate for request: {}", request.getRequestURI());
                return false;
            }
            
            logger.debug("Certificate validation successful for client: {}", certClientId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error during certificate validation: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validate FAPI-specific headers
     */
    private FAPIValidationResult validateFAPIHeaders(HttpServletRequest request) {
        // Validate X-FAPI-Financial-Id
        String financialId = request.getHeader(FAPI_FINANCIAL_ID);
        if (financialId != null && !FINANCIAL_ID_PATTERN.matcher(financialId).matches()) {
            return FAPIValidationResult.invalid("Invalid X-FAPI-Financial-Id format. Expected: CC-AAA-123456");
        }
        
        // Validate X-FAPI-Interaction-Id
        String interactionId = request.getHeader(FAPI_INTERACTION_ID);
        if (interactionId != null && !UUID_PATTERN.matcher(interactionId).matches()) {
            return FAPIValidationResult.invalid("Invalid X-FAPI-Interaction-Id format. Expected: UUID");
        }
        
        // Validate X-FAPI-Customer-IP-Address
        String customerIP = request.getHeader(FAPI_CUSTOMER_IP);
        if (customerIP != null && !IP_PATTERN.matcher(customerIP).matches()) {
            return FAPIValidationResult.invalid("Invalid X-FAPI-Customer-IP-Address format");
        }
        
        // Validate X-FAPI-Auth-Date
        String authDate = request.getHeader(FAPI_AUTH_DATE);
        if (authDate != null) {
            try {
                Instant.parse(authDate);
            } catch (Exception e) {
                return FAPIValidationResult.invalid("Invalid X-FAPI-Auth-Date format. Expected: ISO 8601");
            }
        }
        
        return FAPIValidationResult.valid();
    }
    
    /**
     * Check if the operation is state-changing and requires idempotency
     */
    private boolean isStateChangingOperation(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }
    
    /**
     * Validate idempotency key format and requirements
     */
    private boolean validateIdempotencyKey(HttpServletRequest request) {
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY);
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return false;
        }
        
        // Validate format (should be 16-64 characters, alphanumeric + hyphens/underscores)
        if (idempotencyKey.length() < 16 || idempotencyKey.length() > 64) {
            return false;
        }
        
        if (!idempotencyKey.matches("^[a-zA-Z0-9-_]+$")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Enrich request with security context for downstream services
     */
    private void enrichRequestWithSecurityContext(HttpServletRequest request, HttpServletResponse response) {
        // Generate interaction ID if not provided
        String interactionId = request.getHeader(FAPI_INTERACTION_ID);
        if (interactionId == null) {
            interactionId = UUID.randomUUID().toString();
            response.setHeader(FAPI_INTERACTION_ID, interactionId);
        }
        
        // Set security context attributes
        request.setAttribute("fapi.interactionId", interactionId);
        request.setAttribute("fapi.timestamp", Instant.now().toString());
        
        // Extract client information from certificate
        String clientId = extractClientIdFromCertificate(request);
        if (clientId != null) {
            request.setAttribute("fapi.clientId", clientId);
        }
        
        // Get authenticated user information
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            request.setAttribute("fapi.userId", auth.getName());
            request.setAttribute("fapi.userRoles", auth.getAuthorities().toString());
        }
    }
    
    /**
     * Apply FAPI-required security headers to response
     */
    private void applyFAPISecurityHeaders(HttpServletResponse response) {
        // Security headers for FAPI compliance
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy", "default-src 'self'");
        
        // FAPI-specific headers
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
        response.setHeader("Pragma", "no-cache");
        
        // Add timestamp for audit
        response.setHeader("X-FAPI-Response-Date", Instant.now().toString());
    }
    
    /**
     * Extract client ID from mTLS certificate
     */
    private String extractClientIdFromCertificate(HttpServletRequest request) {
        try {
            Object[] certs = (Object[]) request.getAttribute("javax.servlet.request.X509Certificate");
            if (certs != null && certs.length > 0) {
                X509Certificate clientCert = (X509Certificate) certs[0];
                
                // First try to extract client ID from Subject Alternative Name (SAN)
                String clientIdFromSAN = extractClientIdFromSAN(clientCert);
                if (clientIdFromSAN != null) {
                    return clientIdFromSAN;
                }
                
                // Fallback to extracting from Subject DN
                String subjectDN = clientCert.getSubjectX500Principal().getName();
                return extractClientIdFromSubjectDN(subjectDN);
            }
            
            // Fallback to header-based client ID (for testing/development)
            String headerClientId = request.getHeader(CLIENT_ID);
            if (headerClientId != null && !headerClientId.trim().isEmpty()) {
                logger.debug("Using client ID from header (no certificate): {}", headerClientId);
                return headerClientId;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error extracting client ID from certificate: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Send security error response
     */
    private void sendSecurityError(HttpServletResponse response, String errorCode, 
                                 String errorDescription, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setHeader("X-FAPI-Error-Code", errorCode);
        
        String errorResponse = String.format("""
            {
                "error": "%s",
                "error_description": "%s",
                "timestamp": "%s"
            }
            """, errorCode, errorDescription, Instant.now().toString());
        
        response.getWriter().write(errorResponse);
        response.getWriter().flush();
    }
    
    /**
     * Log security events for audit and monitoring
     */
    private void logSecurityEvent(HttpServletRequest request, String eventType) {
        String clientIP = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        String interactionId = request.getHeader(FAPI_INTERACTION_ID);
        
        // Log to security audit system
        logger.info("FAPI Security Event: {} | IP: {} | InteractionId: {} | UserAgent: {}", 
                   eventType, clientIP, interactionId, userAgent);
        
        // Send to security monitoring system
        // This would integrate with your SIEM/monitoring solution
    }
    
    /**
     * Get client IP address considering proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String clientIP = request.getHeader("X-Forwarded-For");
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = request.getHeader("X-Real-IP");
        }
        if (clientIP == null || clientIP.isEmpty()) {
            clientIP = request.getRemoteAddr();
        }
        
        // Handle comma-separated IPs from X-Forwarded-For
        if (clientIP != null && clientIP.contains(",")) {
            clientIP = clientIP.split(",")[0].trim();
        }
        
        return clientIP;
    }
    
    /**
     * FAPI validation result holder
     */
    private static class FAPIValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private FAPIValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static FAPIValidationResult valid() {
            return new FAPIValidationResult(true, null);
        }
        
        public static FAPIValidationResult invalid(String errorMessage) {
            return new FAPIValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * Validate certificate chain
     */
    private boolean validateCertificateChain(X509Certificate clientCert) {
        try {
            // Check if certificate is self-signed or properly chained
            clientCert.verify(clientCert.getPublicKey());
            
            // In production, this would validate against trusted CA certificates
            // For now, we'll accept self-signed certificates for testing
            logger.debug("Certificate chain validation passed for: {}", 
                clientCert.getSubjectX500Principal().getName());
            return true;
            
        } catch (Exception e) {
            logger.debug("Certificate chain validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check certificate revocation status
     */
    private boolean checkCertificateRevocation(X509Certificate clientCert) {
        try {
            // In production, this would check OCSP or CRL
            // For now, we'll implement a basic validity check
            
            // Check if certificate has CRL distribution points
            byte[] crlDP = clientCert.getExtensionValue("2.5.29.31");
            if (crlDP != null) {
                logger.debug("Certificate has CRL distribution points, should check revocation status");
                // Would perform actual CRL/OCSP check here
            }
            
            // For demo purposes, always return true (not revoked)
            return true;
            
        } catch (Exception e) {
            logger.error("Error checking certificate revocation: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if certificate is currently valid (not expired)
     */
    private boolean isCertificateValid(X509Certificate clientCert) {
        try {
            clientCert.checkValidity();
            return true;
        } catch (Exception e) {
            logger.warn("Certificate validity check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract client ID from Subject Alternative Name
     */
    private String extractClientIdFromSAN(X509Certificate cert) {
        try {
            Collection<List<?>> subjectAltNames = cert.getSubjectAlternativeNames();
            if (subjectAltNames != null) {
                for (List<?> altName : subjectAltNames) {
                    Integer type = (Integer) altName.get(0);
                    String value = (String) altName.get(1);
                    
                    // Look for DNS name or URI that contains client ID
                    if (type == 2 || type == 6) { // DNS name or URI
                        if (value.startsWith("client-") || value.contains("client_id=")) {
                            return extractClientIdFromValue(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting client ID from SAN: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract client ID from Subject Distinguished Name
     */
    private String extractClientIdFromSubjectDN(String subjectDN) {
        try {
            // Parse Subject DN for CN (Common Name)
            String[] parts = subjectDN.split(",");
            for (String part : parts) {
                String trimmedPart = part.trim();
                if (trimmedPart.startsWith("CN=")) {
                    String cn = trimmedPart.substring(3);
                    // Check if CN looks like a client ID
                    if (cn.matches("^[A-Za-z0-9_-]+$")) {
                        return cn;
                    }
                }
                // Also check for custom OID that might contain client ID
                if (trimmedPart.startsWith("1.3.6.1.4.1.")) { // Custom OID
                    return extractClientIdFromValue(trimmedPart);
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting client ID from Subject DN: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extract client ID from a value string
     */
    private String extractClientIdFromValue(String value) {
        // Handle different formats: client-id, client_id=value, etc.
        if (value.startsWith("client-")) {
            return value;
        }
        if (value.contains("client_id=")) {
            String[] parts = value.split("client_id=");
            if (parts.length > 1) {
                return parts[1].split("&")[0]; // Take first part before any other params
            }
        }
        return value;
    }
    
    /**
     * Validate if client ID is valid
     */
    private boolean isValidClientId(String clientId) {
        return clientId != null && 
               !clientId.trim().isEmpty() && 
               clientId.matches("^[A-Za-z0-9_-]{3,50}$");
    }
}