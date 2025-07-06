package com.bank.loan.loan.security.oauth2.controller;

import com.bank.loan.loan.security.par.exception.PARValidationException;
import com.bank.loan.loan.security.par.model.PARRequest;
import com.bank.loan.loan.security.par.service.PARService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class AuthorizationController {
    
    private final PARService parService;
    
    public AuthorizationController(PARService parService) {
        this.parService = parService;
    }
    
    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@RequestParam Map<String, String> parameters,
                                     HttpServletRequest request) {
        
        try {
            // Check if this is a PAR-based authorization request
            String requestUri = parameters.get("request_uri");
            
            if (requestUri == null || requestUri.trim().isEmpty()) {
                // Direct authorization requests are not allowed in FAPI2
                return createErrorResponse("invalid_request", 
                    "Direct authorization requests are not allowed. Use PAR instead.");
            }
            
            // Validate that only request_uri and client_id are provided
            if (parameters.size() > 2 || 
                !parameters.containsKey("client_id") || 
                !parameters.containsKey("request_uri")) {
                return createErrorResponse("invalid_request", 
                    "Only client_id and request_uri parameters are allowed in authorization request");
            }
            
            // Retrieve PAR request
            PARRequest parRequest = parService.retrievePARRequest(requestUri);
            
            // Validate client_id matches
            String clientId = parameters.get("client_id");
            if (!parRequest.getClientId().equals(clientId)) {
                return createErrorResponse("invalid_client", 
                    "Client ID does not match the one used in PAR request");
            }
            
            // Consume the PAR request (one-time use)
            parService.consumePARRequest(requestUri);
            
            // At this point, in a real implementation, you would:
            // 1. Authenticate the user (if not already authenticated)
            // 2. Display consent screen (if needed)
            // 3. Generate authorization code
            // 4. Redirect back to client with authorization code
            
            // For this implementation, we'll simulate a successful authorization
            String authorizationCode = generateAuthorizationCode();
            
            // Build redirect URI with authorization code
            String redirectUri = buildRedirectUri(parRequest, authorizationCode);
            
            // Return redirect response
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUri)
                    .build();
            
        } catch (PARValidationException e) {
            return createErrorResponse(e.getErrorCode(), e.getErrorDescription());
        } catch (Exception e) {
            return createErrorResponse("server_error", "Internal server error occurred");
        }
    }
    
    @GetMapping("/authorize/status")
    public ResponseEntity<Map<String, Object>> getAuthorizationEndpointStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("par_required", true);
        status.put("direct_requests_allowed", false);
        status.put("supported_response_types", new String[]{"code"});
        status.put("supported_response_modes", new String[]{"query"});
        status.put("pkce_required", true);
        status.put("dpop_supported", true);
        
        return ResponseEntity.ok(status);
    }
    
    private ResponseEntity<Map<String, Object>> createErrorResponse(String errorCode, String errorDescription) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        errorResponse.put("error_description", errorDescription);
        
        HttpStatus status;
        switch (errorCode) {
            case "invalid_client":
                status = HttpStatus.UNAUTHORIZED;
                break;
            case "access_denied":
                status = HttpStatus.FORBIDDEN;
                break;
            case "server_error":
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                break;
            default:
                status = HttpStatus.BAD_REQUEST;
        }
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
    
    private String buildRedirectUri(PARRequest parRequest, String authorizationCode) {
        StringBuilder redirectUri = new StringBuilder(parRequest.getRedirectUri());
        
        // Determine if we need to add ? or &
        char separator = parRequest.getRedirectUri().contains("?") ? '&' : '?';
        
        redirectUri.append(separator)
                   .append("code=").append(authorizationCode);
        
        // Add state if present
        if (parRequest.getState() != null && !parRequest.getState().trim().isEmpty()) {
            redirectUri.append("&state=").append(parRequest.getState());
        }
        
        return redirectUri.toString();
    }
    
    private String generateAuthorizationCode() {
        // In a real implementation, this would generate a secure, time-limited authorization code
        // and store it in a database/cache with associated client and user information
        
        java.security.SecureRandom secureRandom = new java.security.SecureRandom();
        byte[] codeBytes = new byte[32];
        secureRandom.nextBytes(codeBytes);
        
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(codeBytes);
    }
}