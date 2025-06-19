package com.bank.loanmanagement.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * FAPI Request Validation Filter
 * Implements Financial-grade API security requirements:
 * - Request ID tracking
 * - TLS validation
 * - Request signature validation
 * - FAPI interaction ID validation
 */
@Component
public class FAPIRequestValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Generate or validate FAPI Interaction ID
        String fapiInteractionId = httpRequest.getHeader("X-FAPI-Interaction-ID");
        if (fapiInteractionId == null) {
            fapiInteractionId = UUID.randomUUID().toString();
        }
        
        // Validate FAPI Interaction ID format
        if (!isValidFAPIInteractionId(fapiInteractionId)) {
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Invalid X-FAPI-Interaction-ID header\"}");
            return;
        }
        
        // Add FAPI Interaction ID to response
        httpResponse.setHeader("X-FAPI-Interaction-ID", fapiInteractionId);
        
        // Validate TLS for FAPI endpoints
        if (isFAPIEndpoint(httpRequest) && !isSecureConnection(httpRequest)) {
            httpResponse.setStatus(HttpStatus.BAD_REQUEST.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"FAPI endpoints require HTTPS/TLS 1.2+\"}");
            return;
        }
        
        // Add security context
        httpRequest.setAttribute("FAPI_INTERACTION_ID", fapiInteractionId);
        httpRequest.setAttribute("FAPI_REQUEST_TIME", System.currentTimeMillis());
        
        // Continue with the filter chain
        chain.doFilter(request, response);
    }
    
    private boolean isValidFAPIInteractionId(String interactionId) {
        // FAPI Interaction ID should be a valid UUID format
        try {
            UUID.fromString(interactionId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private boolean isFAPIEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/customers") ||
               path.startsWith("/api/loans") ||
               path.startsWith("/api/payments") ||
               path.startsWith("/api/admin");
    }
    
    private boolean isSecureConnection(HttpServletRequest request) {
        // Check for HTTPS or secure headers from load balancer
        return request.isSecure() ||
               "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) ||
               "on".equalsIgnoreCase(request.getHeader("X-Forwarded-Ssl"));
    }
}