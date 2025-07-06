package com.bank.loan.loan.security.par.controller;

import com.bank.loan.loan.security.par.exception.PARValidationException;
import com.bank.loan.loan.security.par.model.PARResponse;
import com.bank.loan.loan.security.par.service.PARService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class PARController {
    
    private final PARService parService;
    
    public PARController(PARService parService) {
        this.parService = parService;
    }
    
    @PostMapping(value = "/par", 
                consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> pushAuthorizationRequest(
            @RequestParam MultiValueMap<String, String> parameters,
            HttpServletRequest request) {
        
        try {
            // Convert MultiValueMap to Map (taking first value for each key)
            Map<String, String> requestParams = new HashMap<>();
            parameters.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    requestParams.put(key, values.get(0));
                }
            });
            
            // Log the PAR request for audit purposes
            logPARRequest(requestParams, request);
            
            // Create PAR request
            PARResponse parResponse = parService.createPARRequest(requestParams);
            
            // Return successful response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("request_uri", parResponse.getRequestUri());
            responseBody.put("expires_in", parResponse.getExpiresIn());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseBody);
            
        } catch (PARValidationException e) {
            return handlePARValidationError(e);
        } catch (Exception e) {
            return handleGenericError(e);
        }
    }
    
    @GetMapping("/par/status")
    public ResponseEntity<Map<String, Object>> getPARStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("par_supported", true);
        status.put("par_required", true);
        status.put("par_expires_in_max", 600);
        status.put("par_expires_in_min", 60);
        status.put("par_expires_in_default", 300);
        
        return ResponseEntity.ok(status);
    }
    
    private ResponseEntity<Map<String, Object>> handlePARValidationError(PARValidationException e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getErrorCode());
        errorResponse.put("error_description", e.getErrorDescription());
        
        HttpStatus status;
        switch (e.getErrorCode()) {
            case "invalid_client":
                status = HttpStatus.UNAUTHORIZED;
                break;
            case "unsupported_response_type":
            case "invalid_scope":
            case "invalid_redirect_uri":
                status = HttpStatus.BAD_REQUEST;
                break;
            default:
                status = HttpStatus.BAD_REQUEST;
        }
        
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
    
    private ResponseEntity<Map<String, Object>> handleGenericError(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "server_error");
        errorResponse.put("error_description", "Internal server error occurred");
        
        // Log the actual error for debugging
        System.err.println("PAR endpoint error: " + e.getMessage());
        e.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
    
    private void logPARRequest(Map<String, String> parameters, HttpServletRequest request) {
        // Log PAR request for audit purposes (remove sensitive data)
        Map<String, String> auditParams = new HashMap<>(parameters);
        auditParams.remove("code_challenge"); // Don't log PKCE challenge
        
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        
        System.out.println("PAR Request - Client IP: " + clientIp + 
                          ", User-Agent: " + userAgent + 
                          ", Params: " + auditParams);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}