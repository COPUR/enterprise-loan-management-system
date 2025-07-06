package com.bank.loan.loan.security.interceptor;

import com.bank.loan.loan.security.fapi.annotation.FAPISecured;
import com.bank.loan.loan.security.fapi.validation.FAPISecurityHeaders;
import com.bank.loan.loan.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * FAPI Security Interceptor
 * 
 * Automatically validates FAPI 2.0 security headers for endpoints
 * annotated with @FAPISecured annotation.
 * 
 * Provides centralized FAPI compliance validation across all controllers.
 */
@Component
public class FAPISecurityInterceptor implements HandlerInterceptor {

    @Autowired
    private AuditService auditService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        // Only process controller methods
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check if the controller or method is annotated with @FAPISecured
        FAPISecured classAnnotation = handlerMethod.getBeanType().getAnnotation(FAPISecured.class);
        FAPISecured methodAnnotation = handlerMethod.getMethodAnnotation(FAPISecured.class);
        
        if (classAnnotation == null && methodAnnotation == null) {
            return true; // No FAPI security required
        }

        // Determine which annotation to use (method overrides class)
        FAPISecured fapiSecured = methodAnnotation != null ? methodAnnotation : classAnnotation;

        try {
            // Get FAPI headers
            String fiapiInteractionId = request.getHeader("X-FAPI-Interaction-ID");
            String fapiAuthDate = request.getHeader("X-FAPI-Auth-Date");
            String customerIpAddress = request.getHeader("X-FAPI-Customer-IP-Address");
            String idempotencyKey = request.getHeader("X-Idempotency-Key");

            // Validate required headers based on annotation configuration
            if (fapiSecured.requireInteractionId() && (fiapiInteractionId == null || fiapiInteractionId.trim().isEmpty())) {
                logSecurityViolation("MISSING_FAPI_INTERACTION_ID", "X-FAPI-Interaction-ID header is required", request);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"invalid_request\",\"error_description\":\"X-FAPI-Interaction-ID header is required\"}");
                return false;
            }

            if (fapiSecured.requireCustomerIp() && (customerIpAddress == null || customerIpAddress.trim().isEmpty())) {
                logSecurityViolation("MISSING_FAPI_CUSTOMER_IP", "X-FAPI-Customer-IP-Address header is required", request);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"invalid_request\",\"error_description\":\"X-FAPI-Customer-IP-Address header is required\"}");
                return false;
            }

            if (fapiSecured.requireIdempotencyKey() && (idempotencyKey == null || idempotencyKey.trim().isEmpty())) {
                logSecurityViolation("MISSING_IDEMPOTENCY_KEY", "X-Idempotency-Key header is required", request);
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"invalid_request\",\"error_description\":\"X-Idempotency-Key header is required\"}");
                return false;
            }

            // Validate FAPI headers if required
            if (fapiSecured.requireHeaders()) {
                FAPISecurityHeaders.validateHeaders(fiapiInteractionId, fapiAuthDate, customerIpAddress);
            }

            // Set FAPI Interaction ID in response if provided
            if (fiapiInteractionId != null) {
                response.setHeader("X-FAPI-Interaction-ID", fiapiInteractionId);
            }

            // Log successful FAPI validation
            auditService.logFAPIAuthentication(
                getClientId(request),
                getUserId(),
                extractDPoPJkt(request),
                "FAPI_HEADER_VALIDATION",
                true,
                request.getRemoteAddr(),
                fiapiInteractionId
            );

            return true;

        } catch (FAPISecurityHeaders.FAPISecurityException e) {
            logSecurityViolation("FAPI_HEADER_VALIDATION_FAILED", e.getMessage(), request);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_request\",\"error_description\":\"" + e.getMessage() + "\"}");
            return false;

        } catch (Exception e) {
            logSecurityViolation("FAPI_SECURITY_ERROR", "Unexpected FAPI security validation error: " + e.getMessage(), request);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"server_error\",\"error_description\":\"Internal security validation error\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) 
            throws Exception {
        
        // Log completion of FAPI-secured request
        if (handler instanceof HandlerMethod handlerMethod) {
            FAPISecured classAnnotation = handlerMethod.getBeanType().getAnnotation(FAPISecured.class);
            FAPISecured methodAnnotation = handlerMethod.getMethodAnnotation(FAPISecured.class);
            
            if (classAnnotation != null || methodAnnotation != null) {
                String fiapiInteractionId = request.getHeader("X-FAPI-Interaction-ID");
                
                // Log request completion with security context
                auditService.logFAPIAuthentication(
                    getClientId(request),
                    getUserId(),
                    extractDPoPJkt(request),
                    "FAPI_REQUEST_COMPLETED",
                    response.getStatus() < 400,
                    request.getRemoteAddr(),
                    fiapiInteractionId
                );
            }
        }
    }

    // ========================================================================
    // Private Helper Methods
    // ========================================================================

    private void logSecurityViolation(String violationType, String message, HttpServletRequest request) {
        String fiapiInteractionId = request.getHeader("X-FAPI-Interaction-ID");
        if (fiapiInteractionId == null) {
            fiapiInteractionId = java.util.UUID.randomUUID().toString();
        }

        auditService.logSecurityViolation(violationType, message, getUserId(), 
                                         request.getRemoteAddr(), fiapiInteractionId);
    }

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getClientId(HttpServletRequest request) {
        // Extract client ID from various sources
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("DPoP ")) {
            // In production, this would extract client ID from the DPoP-bound token
            return "extracted_from_token";
        }
        return "unknown_client";
    }

    private String extractDPoPJkt(HttpServletRequest request) {
        String dpopHeader = request.getHeader("DPoP");
        if (dpopHeader != null) {
            // In production, this would extract the JKT thumbprint from the DPoP proof
            return "jkt_thumbprint_from_dpop_proof";
        }
        return null;
    }
}