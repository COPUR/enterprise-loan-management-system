package com.loanmanagement.security.keycloak;

import com.loanmanagement.security.keycloak.dpop.DPoPValidator;
import com.loanmanagement.security.keycloak.fapi.FapiComplianceValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Security filter for DPoP and FAPI 2.0 compliance validation
 * Validates DPoP proofs and FAPI headers for protected endpoints
 */
@Component
public class DPoPSecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(DPoPSecurityFilter.class);
    
    @Autowired
    private DPoPValidator dpopValidator;
    
    @Autowired
    private FapiComplianceValidator fapiValidator;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String DPOP_HEADER = "DPoP";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DPOP_PREFIX = "DPoP ";
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, 
            HttpServletResponse response, 
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Skip validation for public endpoints
            if (isPublicEndpoint(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Validate FAPI compliance
            FapiComplianceValidator.FapiComplianceResult fapiResult = 
                fapiValidator.validateRequest(request);
            
            if (!fapiResult.isCompliant()) {
                sendErrorResponse(response, HttpStatus.BAD_REQUEST, 
                    "fapi_compliance_violation", fapiResult.getViolationMessage());
                return;
            }
            
            // Add FAPI interaction ID to request attributes
            if (fapiResult.getInteractionId() != null) {
                request.setAttribute("fapi.interaction.id", fapiResult.getInteractionId());
            }
            
            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
                
                // Validate DPoP proof if present
                String dpopProof = request.getHeader(DPOP_HEADER);
                if (dpopProof != null) {
                    String accessToken = extractAccessToken(request);
                    
                    DPoPValidator.DPoPValidationResult dpopResult = dpopValidator.validateDPoPProof(
                        dpopProof, 
                        request.getMethod(), 
                        getFullRequestURL(request), 
                        accessToken
                    );
                    
                    if (!dpopResult.isValid()) {
                        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
                            "dpop_validation_failed", dpopResult.getErrorMessage());
                        return;
                    }
                    
                    // Validate token binding
                    Jwt jwt = jwtAuth.getToken();
                    if (!validateTokenBinding(jwt, dpopResult.getJktThumbprint())) {
                        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
                            "token_binding_mismatch", "DPoP token binding validation failed");
                        return;
                    }
                    
                    // Mark DPoP as validated
                    request.setAttribute("dpop.validated", true);
                    request.setAttribute("dpop.jkt", dpopResult.getJktThumbprint());
                    
                    logger.debug("DPoP validation successful for request: {} {}", 
                        request.getMethod(), request.getRequestURI());
                }
                
                // Validate token lifetime for FAPI compliance
                Jwt jwt = jwtAuth.getToken();
                FapiComplianceValidator.FapiComplianceResult tokenResult = 
                    fapiValidator.validateTokenLifetime(jwt.getIssuedAt(), jwt.getExpiresAt());
                
                if (!tokenResult.isCompliant()) {
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, 
                        "fapi_compliance_violation", tokenResult.getViolationMessage());
                    return;
                }
            }
            
            // Add security headers
            addSecurityHeaders(response);
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("DPoP security filter error", e);
            sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, 
                "security_filter_error", "Security validation failed");
        }
    }
    
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Public endpoints that don't require DPoP validation
        return path.startsWith("/actuator/health") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/oauth2/authorization") ||
               path.startsWith("/login") ||
               path.equals("/");
    }
    
    private String extractAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null) {
            if (authHeader.startsWith(BEARER_PREFIX)) {
                return authHeader.substring(BEARER_PREFIX.length());
            } else if (authHeader.startsWith(DPOP_PREFIX)) {
                return authHeader.substring(DPOP_PREFIX.length());
            }
        }
        return null;
    }
    
    private String getFullRequestURL(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        String queryString = request.getQueryString();
        
        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }
    
    private boolean validateTokenBinding(Jwt jwt, String dpopJktThumbprint) {
        // Check if JWT has cnf claim with jkt thumbprint
        Map<String, Object> cnf = jwt.getClaimAsMap("cnf");
        if (cnf == null) {
            logger.warn("JWT does not contain cnf claim for DPoP binding");
            return false;
        }
        
        String jktFromToken = (String) cnf.get("jkt");
        if (jktFromToken == null) {
            logger.warn("JWT cnf claim does not contain jkt thumbprint");
            return false;
        }
        
        boolean matches = jktFromToken.equals(dpopJktThumbprint);
        if (!matches) {
            logger.warn("DPoP token binding mismatch: token jkt={}, proof jkt={}", 
                jktFromToken, dpopJktThumbprint);
        }
        
        return matches;
    }
    
    private void addSecurityHeaders(HttpServletResponse response) {
        // FAPI required security headers
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy", "default-src 'self'");
    }
    
    private void sendErrorResponse(
            HttpServletResponse response, 
            HttpStatus status, 
            String error, 
            String errorDescription) throws IOException {
        
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("error_description", errorDescription);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        response.getWriter().flush();
        
        logger.warn("Security validation failed: {} - {}", error, errorDescription);
    }
}