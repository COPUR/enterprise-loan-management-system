package com.bank.loan.loan.security.dpop.filter;

import com.bank.loan.loan.security.dpop.exception.InvalidDPoPProofException;
import com.bank.loan.loan.security.dpop.exception.TokenBindingMismatchException;
import com.bank.loan.loan.security.dpop.service.DPoPTokenValidationService;
import com.bank.loan.loan.security.dpop.service.DPoPNonceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DPoPValidationFilter extends OncePerRequestFilter {
    
    private static final String DPOP_HEADER = "DPoP";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    private static final String DPOP_NONCE_HEADER = "DPoP-Nonce";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    
    private final DPoPTokenValidationService dpopTokenValidationService;
    private final DPoPNonceService dpopNonceService;
    private final AntPathMatcher pathMatcher;
    
    // Endpoints that don't require DPoP validation
    private final List<String> exemptEndpoints = Arrays.asList(
        "/oauth2/token",
        "/oauth2/par", 
        "/oauth2/authorize",
        "/actuator/health",
        "/actuator/info",
        "/api/public/**",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    );
    
    public DPoPValidationFilter(DPoPTokenValidationService dpopTokenValidationService,
                               DPoPNonceService dpopNonceService) {
        this.dpopTokenValidationService = dpopTokenValidationService;
        this.dpopNonceService = dpopNonceService;
        this.pathMatcher = new AntPathMatcher();
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Skip DPoP validation for exempt endpoints
        if (isExemptEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract headers
            String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
            String dpopHeader = request.getHeader(DPOP_HEADER);
            
            // Validate headers presence
            if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
                sendUnauthorizedResponse(response, "missing_token", "Missing or invalid Authorization header", null, request);
                return;
            }
            
            if (dpopHeader == null || dpopHeader.trim().isEmpty()) {
                sendUnauthorizedResponse(response, "missing_dpop_proof", "Missing DPoP header", null, request);
                return;
            }
            
            // Extract access token
            String accessToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            
            // Construct request URL
            String requestUrl = constructRequestUrl(request);
            String httpMethod = request.getMethod();
            
            // Validate DPoP-bound token
            dpopTokenValidationService.validateDPoPBoundToken(accessToken, dpopHeader, httpMethod, requestUrl);
            
            // If validation succeeds, continue with the request
            filterChain.doFilter(request, response);
            
        } catch (InvalidDPoPProofException e) {
            handleDPoPValidationError(e, response, request);
        } catch (TokenBindingMismatchException e) {
            handleTokenBindingError(e, response, request);
        } catch (Exception e) {
            sendUnauthorizedResponse(response, "invalid_request", "Internal validation error", null, request);
        }
    }
    
    private boolean isExemptEndpoint(String requestPath) {
        return exemptEndpoints.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }
    
    private String constructRequestUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder();
        
        // Check for forwarded headers (e.g., from load balancer)
        String scheme = getForwardedHeader(request, "X-Forwarded-Proto");
        if (scheme == null) {
            scheme = request.getScheme();
        }
        
        String host = getForwardedHeader(request, "X-Forwarded-Host");
        if (host == null) {
            host = request.getServerName();
        }
        
        String port = getForwardedHeader(request, "X-Forwarded-Port");
        int portNumber;
        if (port != null) {
            portNumber = Integer.parseInt(port);
        } else {
            portNumber = request.getServerPort();
        }
        
        url.append(scheme).append("://").append(host);
        
        // Only include port if it's not the default for the scheme
        if (("http".equals(scheme) && portNumber != 80) || 
            ("https".equals(scheme) && portNumber != 443)) {
            url.append(":").append(portNumber);
        }
        
        url.append(request.getRequestURI());
        
        if (request.getQueryString() != null) {
            url.append("?").append(request.getQueryString());
        }
        
        return url.toString();
    }
    
    private String getForwardedHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null && !value.trim().isEmpty()) {
            // Take the first value if there are multiple
            return value.split(",")[0].trim();
        }
        return null;
    }
    
    private void handleDPoPValidationError(InvalidDPoPProofException e, HttpServletResponse response, 
                                         HttpServletRequest request) throws IOException {
        String errorCode = e.getErrorCode();
        String errorDescription = e.getErrorDescription();
        
        if ("use_dpop_nonce".equals(errorCode)) {
            // Generate nonce for client to use
            String nonce = dpopNonceService.generateNonce();
            sendUnauthorizedResponse(response, errorCode, errorDescription, nonce, request);
        } else {
            sendUnauthorizedResponse(response, errorCode, errorDescription, null, request);
        }
    }
    
    private void handleTokenBindingError(TokenBindingMismatchException e, HttpServletResponse response,
                                       HttpServletRequest request) throws IOException {
        String errorCode = e.getErrorCode();
        String errorDescription = e.getErrorDescription();
        
        sendUnauthorizedResponse(response, errorCode, errorDescription, null, request);
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String errorCode, 
                                        String errorDescription, String nonce, 
                                        HttpServletRequest request) throws IOException {
        
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        
        // Set WWW-Authenticate header
        StringBuilder wwwAuth = new StringBuilder("DPoP");
        if (errorCode != null) {
            wwwAuth.append(" error=\"").append(errorCode).append("\"");
        }
        if (errorDescription != null) {
            wwwAuth.append(", error_description=\"").append(errorDescription).append("\"");
        }
        response.setHeader(WWW_AUTHENTICATE_HEADER, wwwAuth.toString());
        
        // Set nonce header if provided
        if (nonce != null) {
            response.setHeader(DPOP_NONCE_HEADER, nonce);
        }
        
        // Set CORS headers if request has Origin header
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, 
                WWW_AUTHENTICATE_HEADER + ", " + DPOP_NONCE_HEADER);
        }
        
        // Send JSON error response
        String jsonResponse = String.format(
            "{\"error\":\"%s\",\"error_description\":\"%s\"}", 
            errorCode != null ? errorCode : "unauthorized",
            errorDescription != null ? errorDescription : "Unauthorized"
        );
        
        response.getWriter().write(jsonResponse);
    }
}