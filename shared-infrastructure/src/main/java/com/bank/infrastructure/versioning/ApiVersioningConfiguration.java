package com.bank.infrastructure.versioning;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.web.accept.ParameterContentNegotiationStrategy;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * API Versioning Configuration for Banking Platform
 * 
 * Implements comprehensive API versioning strategy:
 * - URL-based versioning (/api/v1/, /api/v2/)
 * - Header-based versioning (Accept: application/vnd.banking.v1+json)
 * - Media type versioning with content negotiation
 * - Backward compatibility support
 * - Version deprecation warnings
 * - Automatic version detection and routing
 */
@Configuration
public class ApiVersioningConfiguration implements WebMvcConfigurer {
    
    // Supported API versions
    public static final String VERSION_1 = "1.0";
    public static final String VERSION_2 = "2.0";
    public static final String CURRENT_VERSION = VERSION_2;
    
    // Custom media types for versioning
    public static final MediaType BANKING_V1_JSON = MediaType.parseMediaType("application/vnd.banking.v1+json");
    public static final MediaType BANKING_V2_JSON = MediaType.parseMediaType("application/vnd.banking.v2+json");
    public static final MediaType BANKING_LATEST_JSON = MediaType.parseMediaType("application/vnd.banking+json");
    
    // Version patterns
    private static final Pattern URL_VERSION_PATTERN = Pattern.compile("/api/v(\\d+(?:\\.\\d+)?)/.*");
    private static final Pattern HEADER_VERSION_PATTERN = Pattern.compile("application/vnd\\.banking\\.v(\\d+(?:\\.\\d+)?)\\+json");
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(false)
            .favorPathExtension(false)
            .ignoreAcceptHeader(false)
            .useRegisteredExtensionsOnly(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("v1", BANKING_V1_JSON)
            .mediaType("v2", BANKING_V2_JSON)
            .mediaType("latest", BANKING_LATEST_JSON);
    }
    
    @Bean
    public ContentNegotiationStrategy versionContentNegotiationStrategy() {
        return new BankingVersionContentNegotiationStrategy();
    }
    
    @Bean
    public ApiVersionInterceptor apiVersionInterceptor() {
        return new ApiVersionInterceptor();
    }
    
    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor())
                .addPathPatterns("/api/**");
    }
    
    /**
     * Custom content negotiation strategy for API versioning
     */
    private static class BankingVersionContentNegotiationStrategy implements ContentNegotiationStrategy {
        
        @Override
        public List<MediaType> resolveMediaTypes(org.springframework.web.context.request.NativeWebRequest request) 
                throws org.springframework.web.HttpMediaTypeNotAcceptableException {
            
            HttpServletRequest servletRequest = request.getNativeRequest(HttpServletRequest.class);
            if (servletRequest == null) {
                return List.of(MediaType.APPLICATION_JSON);
            }
            
            // Try to determine version from URL first
            String version = extractVersionFromUrl(servletRequest.getRequestURI());
            if (version != null) {
                return List.of(getMediaTypeForVersion(version));
            }
            
            // Try to determine version from Accept header
            String acceptHeader = servletRequest.getHeader("Accept");
            if (acceptHeader != null) {
                version = extractVersionFromHeader(acceptHeader);
                if (version != null) {
                    return List.of(getMediaTypeForVersion(version));
                }
            }
            
            // Default to latest version
            return List.of(BANKING_LATEST_JSON);
        }
        
        private String extractVersionFromUrl(String uri) {
            Matcher matcher = URL_VERSION_PATTERN.matcher(uri);
            return matcher.matches() ? matcher.group(1) : null;
        }
        
        private String extractVersionFromHeader(String acceptHeader) {
            Matcher matcher = HEADER_VERSION_PATTERN.matcher(acceptHeader);
            return matcher.find() ? matcher.group(1) : null;
        }
        
        private MediaType getMediaTypeForVersion(String version) {
            switch (version) {
                case "1":
                case "1.0":
                    return BANKING_V1_JSON;
                case "2":
                case "2.0":
                    return BANKING_V2_JSON;
                default:
                    return BANKING_LATEST_JSON;
            }
        }
    }
    
    /**
     * Interceptor for API version handling and deprecation warnings
     */
    private static class ApiVersionInterceptor implements org.springframework.web.servlet.HandlerInterceptor {
        
        @Override
        public boolean preHandle(HttpServletRequest request, 
                               jakarta.servlet.http.HttpServletResponse response,
                               Object handler) throws Exception {
            
            // Extract and validate API version
            ApiVersionInfo versionInfo = extractVersionInfo(request);
            
            // Set version information in request attributes
            request.setAttribute("api.version", versionInfo.getVersion());
            request.setAttribute("api.version.source", versionInfo.getSource());
            
            // Add version headers to response
            response.setHeader("X-API-Version", versionInfo.getVersion());
            response.setHeader("X-API-Version-Source", versionInfo.getSource().name());
            response.setHeader("X-API-Current-Version", CURRENT_VERSION);
            
            // Add deprecation warnings for old versions
            if (isDeprecatedVersion(versionInfo.getVersion())) {
                response.setHeader("X-API-Deprecated", "true");
                response.setHeader("X-API-Deprecation-Date", getDeprecationDate(versionInfo.getVersion()));
                response.setHeader("X-API-Sunset-Date", getSunsetDate(versionInfo.getVersion()));
                response.setHeader("Warning", "299 - \"API version " + versionInfo.getVersion() + 
                                             " is deprecated. Please upgrade to version " + CURRENT_VERSION + "\"");
            }
            
            // Log version usage for analytics
            logVersionUsage(versionInfo, request);
            
            return true;
        }
        
        @Override
        public void postHandle(HttpServletRequest request, 
                             jakarta.servlet.http.HttpServletResponse response,
                             Object handler, 
                             org.springframework.web.servlet.ModelAndView modelAndView) throws Exception {
            
            // Add HATEOAS links with proper versioning
            addVersionedHateoasHeaders(request, response);
        }
        
        private ApiVersionInfo extractVersionInfo(HttpServletRequest request) {
            // Try URL-based versioning first
            String uri = request.getRequestURI();
            Matcher urlMatcher = URL_VERSION_PATTERN.matcher(uri);
            if (urlMatcher.matches()) {
                return new ApiVersionInfo(urlMatcher.group(1), VersionSource.URL);
            }
            
            // Try header-based versioning
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null) {
                Matcher headerMatcher = HEADER_VERSION_PATTERN.matcher(acceptHeader);
                if (headerMatcher.find()) {
                    return new ApiVersionInfo(headerMatcher.group(1), VersionSource.HEADER);
                }
            }
            
            // Try custom version header
            String versionHeader = request.getHeader("X-API-Version");
            if (versionHeader != null && !versionHeader.trim().isEmpty()) {
                return new ApiVersionInfo(versionHeader.trim(), VersionSource.CUSTOM_HEADER);
            }
            
            // Default to current version
            return new ApiVersionInfo(CURRENT_VERSION, VersionSource.DEFAULT);
        }
        
        private boolean isDeprecatedVersion(String version) {
            // Version 1.x is deprecated
            return version.startsWith("1.");
        }
        
        private String getDeprecationDate(String version) {
            // Implementation would return actual deprecation dates
            switch (version) {
                case "1.0":
                    return "2024-01-01";
                default:
                    return "Unknown";
            }
        }
        
        private String getSunsetDate(String version) {
            // Implementation would return actual sunset dates
            switch (version) {
                case "1.0":
                    return "2024-12-31";
                default:
                    return "Unknown";
            }
        }
        
        private void logVersionUsage(ApiVersionInfo versionInfo, HttpServletRequest request) {
            String clientIP = getClientIP(request);
            String userAgent = request.getHeader("User-Agent");
            String endpoint = request.getMethod() + " " + request.getRequestURI();
            
            // Log version usage for analytics and monitoring
            System.out.println(String.format(
                "API_VERSION_USAGE: Version=%s Source=%s Endpoint=%s IP=%s UserAgent=%s",
                versionInfo.getVersion(), versionInfo.getSource(), endpoint, clientIP, userAgent
            ));
            
            // In production, this would integrate with your analytics system
        }
        
        private void addVersionedHateoasHeaders(HttpServletRequest request, 
                                              jakarta.servlet.http.HttpServletResponse response) {
            String version = (String) request.getAttribute("api.version");
            if (version != null) {
                response.setHeader("X-HATEOAS-Version", version);
                response.setHeader("Link", String.format(
                    "</api/v%s/docs>; rel=\"documentation\", " +
                    "</api/v%s/openapi.json>; rel=\"describedby\", " +
                    "</api/v%s>; rel=\"version\"",
                    version, version, CURRENT_VERSION
                ));
            }
        }
        
        private String getClientIP(HttpServletRequest request) {
            String clientIP = request.getHeader("X-Forwarded-For");
            if (clientIP == null || clientIP.isEmpty()) {
                clientIP = request.getHeader("X-Real-IP");
            }
            if (clientIP == null || clientIP.isEmpty()) {
                clientIP = request.getRemoteAddr();
            }
            if (clientIP != null && clientIP.contains(",")) {
                clientIP = clientIP.split(",")[0].trim();
            }
            return clientIP;
        }
    }
    
    /**
     * API Version Information holder
     */
    private static class ApiVersionInfo {
        private final String version;
        private final VersionSource source;
        
        public ApiVersionInfo(String version, VersionSource source) {
            this.version = version;
            this.source = source;
        }
        
        public String getVersion() { return version; }
        public VersionSource getSource() { return source; }
    }
    
    /**
     * Version source enumeration
     */
    private enum VersionSource {
        URL,
        HEADER,
        CUSTOM_HEADER,
        DEFAULT
    }
}

/**
 * Version-aware request mapping annotation
 */
@java.lang.annotation.Target({java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@java.lang.annotation.Documented
@interface ApiVersion {
    /**
     * Supported versions for this endpoint
     */
    String[] value() default {};
    
    /**
     * Minimum supported version
     */
    String since() default "1.0";
    
    /**
     * Deprecated since version
     */
    String deprecated() default "";
    
    /**
     * Removed in version
     */
    String removed() default "";
}

/**
 * Utility class for version comparison and validation
 */
class VersionUtils {
    
    /**
     * Compare two version strings
     */
    public static int compareVersions(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (part1 < part2) return -1;
            if (part1 > part2) return 1;
        }
        
        return 0;
    }
    
    /**
     * Check if version is supported
     */
    public static boolean isVersionSupported(String version) {
        return compareVersions(version, "1.0") >= 0 && 
               compareVersions(version, ApiVersioningConfiguration.CURRENT_VERSION) <= 0;
    }
    
    /**
     * Get the latest compatible version
     */
    public static String getLatestCompatibleVersion(String requestedVersion) {
        if (!isVersionSupported(requestedVersion)) {
            return ApiVersioningConfiguration.CURRENT_VERSION;
        }
        return requestedVersion;
    }
}