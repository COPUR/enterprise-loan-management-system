package com.bank.infrastructure.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Security Headers Interceptor
 * 
 * Adds comprehensive security headers to all API responses:
 * - FAPI 2.0 compliance headers
 * - OWASP security headers
 * - Content Security Policy
 * - Banking-specific security headers
 */
@Component
public class SecurityHeadersInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // FAPI 2.0 Security Headers
        response.setHeader("X-FAPI-Interaction-ID", 
            request.getHeader("X-FAPI-Interaction-ID") != null 
                ? request.getHeader("X-FAPI-Interaction-ID")
                : java.util.UUID.randomUUID().toString());
        
        response.setHeader("X-FAPI-Auth-Date", 
            java.time.Instant.now().toString());
        
        // OWASP Security Headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", 
            "geolocation=(), microphone=(), camera=(), payment=()");
        
        // Content Security Policy
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' https:; " +
            "connect-src 'self' https:; " +
            "frame-ancestors 'none'");
        
        // HSTS (HTTP Strict Transport Security)
        response.setHeader("Strict-Transport-Security", 
            "max-age=63072000; includeSubDomains; preload");
        
        // Banking-specific security headers
        response.setHeader("X-Banking-API-Version", "v1.0.0");
        response.setHeader("X-Banking-Environment", 
            System.getProperty("spring.profiles.active", "development"));
        
        // Cache control for sensitive data
        if (request.getRequestURI().contains("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
        }
        
        return true;
    }
}