package com.bank.infrastructure.api.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * API Versioning Interceptor
 * 
 * Handles API versioning with multiple strategies:
 * - URL-based versioning: /api/v1/customers
 * - Header-based versioning: Accept: application/vnd.banking.v1+json
 * - Query parameter versioning: ?version=v1
 * - Version deprecation warnings
 * - Backward compatibility support
 */
@Component
public class ApiVersioningInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiVersioningInterceptor.class);
    
    private static final String VERSION_HEADER = "X-API-Version";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String VERSION_PARAM = "version";
    
    private static final Pattern URL_VERSION_PATTERN = Pattern.compile("/api/v(\\d+)/");
    private static final Pattern ACCEPT_VERSION_PATTERN = Pattern.compile("application/vnd\\.banking\\.v(\\d+)\\+json");
    
    // Supported API versions
    private static final String CURRENT_VERSION = "v1";
    private static final String[] SUPPORTED_VERSIONS = {"v1"};
    private static final String[] DEPRECATED_VERSIONS = {}; // Add deprecated versions here
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String requestedVersion = extractVersion(request);
            
            // Validate version
            if (!isVersionSupported(requestedVersion)) {
                handleUnsupportedVersion(request, response, requestedVersion);
                return false;
            }
            
            // Check for deprecated version
            if (isVersionDeprecated(requestedVersion)) {
                addDeprecationWarning(response, requestedVersion);
            }
            
            // Set version information in response
            response.setHeader(VERSION_HEADER, requestedVersion);
            
            // Store version in request attributes for controllers
            request.setAttribute("apiVersion", requestedVersion);
            
            logger.debug("API request processed with version: {}", requestedVersion);
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error in API versioning interceptor", e);
            // Fail open with current version
            request.setAttribute("apiVersion", CURRENT_VERSION);
            response.setHeader(VERSION_HEADER, CURRENT_VERSION);
            return true;
        }
    }
    
    private String extractVersion(HttpServletRequest request) {
        // Priority: URL > Header > Query Parameter > Default
        
        // 1. Check URL-based versioning
        String urlVersion = extractVersionFromUrl(request.getRequestURI());
        if (urlVersion != null) {
            return urlVersion;
        }
        
        // 2. Check header-based versioning
        String headerVersion = extractVersionFromHeader(request);
        if (headerVersion != null) {
            return headerVersion;
        }
        
        // 3. Check query parameter versioning
        String paramVersion = request.getParameter(VERSION_PARAM);
        if (paramVersion != null && !paramVersion.isEmpty()) {
            return normalizeVersion(paramVersion);
        }
        
        // 4. Default to current version
        return CURRENT_VERSION;
    }
    
    private String extractVersionFromUrl(String uri) {
        var matcher = URL_VERSION_PATTERN.matcher(uri);
        if (matcher.find()) {
            return "v" + matcher.group(1);
        }
        return null;
    }
    
    private String extractVersionFromHeader(HttpServletRequest request) {
        // Check X-API-Version header
        String versionHeader = request.getHeader(VERSION_HEADER);
        if (versionHeader != null && !versionHeader.isEmpty()) {
            return normalizeVersion(versionHeader);
        }
        
        // Check Accept header for media type versioning
        String acceptHeader = request.getHeader(ACCEPT_HEADER);
        if (acceptHeader != null) {
            var matcher = ACCEPT_VERSION_PATTERN.matcher(acceptHeader);
            if (matcher.find()) {
                return "v" + matcher.group(1);
            }
        }
        
        return null;
    }
    
    private String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return CURRENT_VERSION;
        }
        
        // Remove any non-alphanumeric characters and normalize
        version = version.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        
        // Ensure it starts with 'v'
        if (!version.startsWith("v")) {
            version = "v" + version;
        }
        
        return version;
    }
    
    private boolean isVersionSupported(String version) {
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            if (supportedVersion.equals(version)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isVersionDeprecated(String version) {
        for (String deprecatedVersion : DEPRECATED_VERSIONS) {
            if (deprecatedVersion.equals(version)) {
                return true;
            }
        }
        return false;
    }
    
    private void addDeprecationWarning(HttpServletResponse response, String version) {
        response.setHeader("Warning", 
            String.format("299 - \"API version %s is deprecated. Please upgrade to %s\"", 
                version, CURRENT_VERSION));
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", "2024-12-31T23:59:59Z"); // Example sunset date
        
        logger.warn("Deprecated API version {} requested", version);
    }
    
    private void handleUnsupportedVersion(HttpServletRequest request, HttpServletResponse response, 
                                         String requestedVersion) {
        response.setStatus(HttpStatus.NOT_ACCEPTABLE.value());
        response.setContentType("application/problem+json");
        
        try {
            String responseBody = String.format("""
                {
                    "type": "https://banking.example.com/problems/unsupported-version",
                    "title": "Unsupported API Version",
                    "status": 406,
                    "detail": "The requested API version '%s' is not supported.",
                    "instance": "%s",
                    "timestamp": "%s",
                    "requestId": "%s",
                    "supportedVersions": [%s],
                    "currentVersion": "%s"
                }
                """,
                requestedVersion,
                request.getRequestURI(),
                java.time.Instant.now().toString(),
                request.getAttribute("requestId"),
                String.join("\",\"", SUPPORTED_VERSIONS).replaceAll("^|$", "\""),
                CURRENT_VERSION
            );
            
            response.getWriter().write(responseBody);
        } catch (Exception e) {
            logger.error("Failed to write unsupported version response", e);
        }
        
        logger.warn("Unsupported API version {} requested for URI: {}", 
            requestedVersion, request.getRequestURI());
    }
    
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
    
    public static String[] getSupportedVersions() {
        return SUPPORTED_VERSIONS.clone();
    }
    
    public static String[] getDeprecatedVersions() {
        return DEPRECATED_VERSIONS.clone();
    }
}